package com.nexpay.wallet.domain.exception;

/**
 * Thrown when a wallet already exists for a given email.
 */
public class WalletAlreadyExistsException extends RuntimeException {

    public WalletAlreadyExistsException(String email) {
        super("Wallet already exists for email: " + email);
    }
}
