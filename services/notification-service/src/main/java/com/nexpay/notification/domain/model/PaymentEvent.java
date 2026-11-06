package com.nexpay.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event model from SQS payment-events queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventType;
    private UUID paymentId;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String failureReason;
    private Instant timestamp;
}
