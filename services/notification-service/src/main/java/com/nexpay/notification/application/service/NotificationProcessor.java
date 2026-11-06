package com.nexpay.notification.application.service;

import com.nexpay.notification.domain.model.PaymentEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification processor that handles different payment event types.
 *
 * In production, each handler would integrate with:
 * - Email service (SendGrid, SES)
 * - Push notifications (Firebase)
 * - SMS (Twilio)
 *
 * Currently logs and publishes to SNS topics.
 */
@Slf4j
@Service
public class NotificationProcessor {

    private final Counter emailsSentCounter;
    private final Counter pushSentCounter;

    public NotificationProcessor(MeterRegistry meterRegistry) {
        this.emailsSentCounter = Counter.builder("nexpay.notifications.emails.sent")
                .description("Emails sent").register(meterRegistry);
        this.pushSentCounter = Counter.builder("nexpay.notifications.push.sent")
                .description("Push notifications sent").register(meterRegistry);
    }

    public void process(PaymentEvent event) {
        log.info("Processing notification for event: {} | Payment: {}",
                event.getEventType(), event.getPaymentId());

        switch (event.getEventType()) {
            case "PAYMENT_COMPLETED" -> handlePaymentCompleted(event);
            case "PAYMENT_FAILED" -> handlePaymentFailed(event);
            case "PAYMENT_REFUNDED" -> handlePaymentRefunded(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handlePaymentCompleted(PaymentEvent event) {
        // Notify sender: "Payment of $X sent successfully"
        log.info("[EMAIL] [Sender {}] Payment of {} {} sent to wallet {} successfully. Payment ID: {}",
                event.getSenderWalletId(),
                event.getAmount(), event.getCurrency(),
                event.getReceiverWalletId(),
                event.getPaymentId());

        // Notify receiver: "You received $X"
        log.info("[EMAIL] [Receiver {}] You received {} {} from wallet {}. Payment ID: {}",
                event.getReceiverWalletId(),
                event.getAmount(), event.getCurrency(),
                event.getSenderWalletId(),
                event.getPaymentId());

        // Push notification
        log.info("[PUSH] [Receiver {}] You received {} {}!",
                event.getReceiverWalletId(),
                event.getAmount(), event.getCurrency());

        emailsSentCounter.increment(2); // sender + receiver
        pushSentCounter.increment();
    }

    private void handlePaymentFailed(PaymentEvent event) {
        log.warn("[EMAIL] [Sender {}] Payment of {} {} FAILED. Reason: {}. Payment ID: {}",
                event.getSenderWalletId(),
                event.getAmount(), event.getCurrency(),
                event.getFailureReason(),
                event.getPaymentId());

        log.warn("[ALERT] Payment {} failed, investigating. Reason: {}",
                event.getPaymentId(), event.getFailureReason());

        emailsSentCounter.increment();
    }

    private void handlePaymentRefunded(PaymentEvent event) {
        log.info("[EMAIL] [Sender {}] Your payment of {} {} has been refunded. Payment ID: {}",
                event.getSenderWalletId(),
                event.getAmount(), event.getCurrency(),
                event.getPaymentId());

        log.info("[EMAIL] [Receiver {}] A payment of {} {} has been reversed from your wallet. Payment ID: {}",
                event.getReceiverWalletId(),
                event.getAmount(), event.getCurrency(),
                event.getPaymentId());

        emailsSentCounter.increment(2);
    }
}
