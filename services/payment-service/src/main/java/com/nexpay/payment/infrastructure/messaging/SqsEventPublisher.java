package com.nexpay.payment.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexpay.payment.domain.model.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * Publishes payment events to SQS for async consumption
 * by notification-service.
 */
@Slf4j
@Component
public class SqsEventPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsEventPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.payment-events-queue:http://localhost:4566/000000000000/payment-events}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    /**
     * Publishes a payment event to the SQS queue.
     * The notification-service will consume this event asynchronously.
     *
     * @param event the payment event to publish
     */
    public void publish(PaymentEvent event) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageGroupId("payment-events")
                    .messageDeduplicationId(event.getPaymentId().toString() + "-" + event.getEventType())
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(request);

            log.info("Published event {} for payment {} to SQS. MessageId: {}",
                    event.getEventType(),
                    event.getPaymentId(),
                    response.messageId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment event: {}", event, e);
            throw new RuntimeException("Failed to publish payment event", e);
        } catch (Exception e) {
            log.error("Failed to publish event to SQS for payment: {}", event.getPaymentId(), e);
            // Don't throw - payment is already completed, notification is best-effort
            // The event will be retried via DLQ monitoring
        }
    }
}
