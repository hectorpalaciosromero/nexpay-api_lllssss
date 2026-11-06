package com.nexpay.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a digital wallet.
 */
@Getter
@Builder
@AllArgsConstructor
public class Wallet {

    private UUID id;
    private String ownerEmail;
    private String ownerName;
    private String currency;
    private BigDecimal balance;
    private WalletStatus status;
    private Long version; // Optimistic locking
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Credits funds to the wallet.
     * @throws IllegalStateException if wallet is not active
     */
    public void credit(BigDecimal amount) {
        validateActive();
        validatePositiveAmount(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * Debits funds from the wallet.
     * @throws IllegalStateException if wallet is not active
     * @throws IllegalArgumentException if insufficient balance
     */
    public void debit(BigDecimal amount) {
        validateActive();
        validatePositiveAmount(amount);
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    String.format("Insufficient balance. Current: %s, Requested: %s", this.balance, amount));
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Activates the wallet.
     */
    public void activate() {
        if (this.status == WalletStatus.CLOSED) {
            throw new IllegalStateException("Cannot activate a closed wallet");
        }
        this.status = WalletStatus.ACTIVE;
    }

    /**
     * Deactivates the wallet.
     */
    public void deactivate() {
        this.status = WalletStatus.INACTIVE;
    }

    /**
     * Blocks the wallet (e.g., due to fraud).
     */
    public void block() {
        this.status = WalletStatus.BLOCKED;
    }

    /**
     * Permanently closes the wallet.
     * @throws IllegalStateException if wallet has remaining balance
     */
    public void close() {
        if (this.balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot close wallet with remaining balance: " + this.balance);
        }
        this.status = WalletStatus.CLOSED;
    }

    private void validateActive() {
        if (this.status != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active. Current status: " + this.status);
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }
}
