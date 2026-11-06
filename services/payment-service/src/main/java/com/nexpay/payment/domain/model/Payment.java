package com.nexpay.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for a P2P payment/transfer.
 * Contains business rules for payment state transitions.
 */
@Getter
@Builder
@AllArgsConstructor
public class Payment {

    private UUID id;
    private String idempotencyKey;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    /**
     * Marks payment as processing (Saga step 1: debit sender).
     */
    public void startProcessing() {
        validateStatus(PaymentStatus.PENDING);
        this.status = PaymentStatus.PROCESSING;
    }

    /**
     * Marks payment as completed (Saga step 2: credit receiver done).
     */
    public void complete() {
        validateStatus(PaymentStatus.PROCESSING);
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Marks payment as failed with reason (triggers Saga compensation).
     */
    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Initiates refund process.
     */
    public void initiateRefund() {
        validateStatus(PaymentStatus.COMPLETED);
        this.status = PaymentStatus.REFUND_PENDING;
    }

    /**
     * Completes the refund.
     */
    public void refund() {
        validateStatus(PaymentStatus.REFUND_PENDING);
        this.status = PaymentStatus.REFUNDED;
    }

    private void validateStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("Invalid status transition. Current: %s, Expected: %s", this.status, expected));
        }
    }
}
