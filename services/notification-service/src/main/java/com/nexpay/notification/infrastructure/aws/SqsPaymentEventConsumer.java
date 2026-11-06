package com.nexpay.notification.infrastructure.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexpay.notification.application.service.NotificationProcessor;
import com.nexpay.notification.domain.model.PaymentEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

/**
 * SQS consumer that polls the payment-events queue.
 * Supports Dead Letter Queue for failed messages.
 *
 * Flow:
 * 1. Poll SQS queue every 5 seconds
 * 2. Deserialize PaymentEvent
 * 3. Process via NotificationProcessor
 * 4. Delete message on success (ack)
 * 5. On failure: message stays in queue, after 3 retries goes to DLQ
 */
@Slf4j
@Component
public class SqsPaymentEventConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final NotificationProcessor notificationProcessor;
    private final String queueUrl;
    private final Counter eventsProcessedCounter;
    private final Counter eventsFailedCounter;

    public SqsPaymentEventConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            NotificationProcessor notificationProcessor,
            @Value("${aws.sqs.payment-events-url}") String queueUrl,
            MeterRegistry meterRegistry) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.notificationProcessor = notificationProcessor;
        this.queueUrl = queueUrl;

        this.eventsProcessedCounter = Counter.builder("nexpay.notifications.processed")
                .description("Events successfully processed").register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("nexpay.notifications.failed")
                .description("Events that failed processing").register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${aws.sqs.poll-interval-ms:5000}")
    public void pollMessages() {
        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(5)  // Long polling
                            .visibilityTimeout(30)
                            .build()
            );

            List<Message> messages = response.messages();
            if (!messages.isEmpty()) {
                log.info("Received {} messages from SQS", messages.size());
            }

            for (Message message : messages) {
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("Error polling SQS: {}", e.getMessage());
        }
    }

    private void processMessage(Message message) {
        try {
            log.info("Processing message: {}", message.messageId());

            // Parse the event - handle SNS wrapper if present
            String body = message.body();
            PaymentEvent event;

            if (body.contains("\"Type\"") && body.contains("\"Message\"")) {
                // Message came through SNS, extract inner message
                var snsWrapper = objectMapper.readTree(body);
                event = objectMapper.readValue(
                        snsWrapper.get("Message").asText(), PaymentEvent.class);
            } else {
                event = objectMapper.readValue(body, PaymentEvent.class);
            }

            // Process the notification
            notificationProcessor.process(event);

            // Acknowledge: delete from queue
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

            eventsProcessedCounter.increment();
            log.info("Successfully processed and acknowledged message: {}", message.messageId());

        } catch (Exception e) {
            eventsFailedCounter.increment();
            log.error("Failed to process message {}: {}. Will retry or go to DLQ.",
                    message.messageId(), e.getMessage());
            // Don't delete - SQS will retry, then send to DLQ after maxReceiveCount
        }
    }
}
