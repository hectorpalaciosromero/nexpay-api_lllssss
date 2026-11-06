package com.nexpay.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published to SQS when a payment state changes.
 * Consumed by notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventType;   // PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_REFUNDED
    private UUID paymentId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String failureReason;
    private Instant timestamp;
}
