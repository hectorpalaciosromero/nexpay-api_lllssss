package com.nexpay.notification.unit;

import com.nexpay.notification.application.service.NotificationProcessor;
import com.nexpay.notification.domain.model.PaymentEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Notification Service Unit Tests")
class NotificationProcessorTest {

    private NotificationProcessor notificationProcessor;

    @BeforeEach
    void setUp() {
        notificationProcessor = new NotificationProcessor(new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("Process Payment Events")
    class ProcessPaymentEventTests {

        @Test
        @DisplayName("Should process PAYMENT_COMPLETED event successfully")
        void shouldProcessCompletedEvent() {
            PaymentEvent event = createEvent("PAYMENT_COMPLETED");

            assertThatCode(() -> notificationProcessor.process(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should process PAYMENT_FAILED event successfully")
        void shouldProcessFailedEvent() {
            PaymentEvent event = createEvent("PAYMENT_FAILED");
            event.setFailureReason("Insufficient funds");

            assertThatCode(() -> notificationProcessor.process(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should process PAYMENT_REFUNDED event successfully")
        void shouldProcessRefundedEvent() {
            PaymentEvent event = createEvent("PAYMENT_REFUNDED");

            assertThatCode(() -> notificationProcessor.process(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle unknown event type gracefully")
        void shouldHandleUnknownEventType() {
            PaymentEvent event = createEvent("UNKNOWN_EVENT");

            assertThatCode(() -> notificationProcessor.process(event))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("PaymentEvent Domain Model")
    class PaymentEventModelTests {

        @Test
        @DisplayName("Should create valid PaymentEvent with builder")
        void shouldCreateValidPaymentEvent() {
            UUID paymentId = UUID.randomUUID();
            UUID senderWalletId = UUID.randomUUID();
            UUID receiverWalletId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("150.00");

            PaymentEvent event = PaymentEvent.builder()
                    .paymentId(paymentId)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(amount)
                    .currency("USD")
                    .eventType("PAYMENT_COMPLETED")
                    .timestamp(Instant.now())
                    .build();

            assertThat(event.getPaymentId()).isEqualTo(paymentId);
            assertThat(event.getSenderWalletId()).isEqualTo(senderWalletId);
            assertThat(event.getReceiverWalletId()).isEqualTo(receiverWalletId);
            assertThat(event.getAmount()).isEqualByComparingTo(amount);
            assertThat(event.getEventType()).isEqualTo("PAYMENT_COMPLETED");
        }

        @Test
        @DisplayName("Should support NoArgsConstructor and setters")
        void shouldSupportSetters() {
            PaymentEvent event = new PaymentEvent();
            event.setEventType("PAYMENT_FAILED");
            event.setPaymentId(UUID.randomUUID());
            event.setAmount(new BigDecimal("75.00"));

            assertThat(event.getEventType()).isEqualTo("PAYMENT_FAILED");
            assertThat(event.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        }
    }

    private PaymentEvent createEvent(String eventType) {
        return PaymentEvent.builder()
                .paymentId(UUID.randomUUID())
                .senderWalletId(UUID.randomUUID())
                .receiverWalletId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .eventType(eventType)
                .timestamp(Instant.now())
                .build();
    }
}
