package com.nexpay.payment.domain.model;

/**
 * PENDING -> PROCESSING -> COMPLETED
 *                       -> FAILED
 * COMPLETED -> REFUND_PENDING -> REFUNDED
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUND_PENDING,
    REFUNDED
}
