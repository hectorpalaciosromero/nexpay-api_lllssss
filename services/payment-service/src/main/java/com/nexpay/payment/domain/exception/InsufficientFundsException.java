package com.nexpay.payment.domain.exception;

/**
 * Thrown when a payment cannot be processed due to insufficient funds.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String walletId) {
        super("Insufficient funds in wallet: " + walletId);
    }
}
