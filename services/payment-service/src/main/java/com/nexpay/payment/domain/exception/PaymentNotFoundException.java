package com.nexpay.payment.domain.exception;

/**
 * Thrown when a payment is not found.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String id) {
        super("Payment not found with id: " + id);
    }
}
