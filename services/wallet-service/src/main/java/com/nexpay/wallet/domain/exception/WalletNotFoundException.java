package com.nexpay.wallet.domain.exception;

/**
 * Thrown when a wallet is not found by ID or email.
 */
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String message) {
        super(message);
    }

    public static WalletNotFoundException byId(java.util.UUID id) {
        return new WalletNotFoundException("Wallet not found with id: " + id);
    }

    public static WalletNotFoundException byEmail(String email) {
        return new WalletNotFoundException("Wallet not found for email: " + email);
    }
}
