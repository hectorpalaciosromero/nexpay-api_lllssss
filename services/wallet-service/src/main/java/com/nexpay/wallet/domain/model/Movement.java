package com.nexpay.wallet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a financial movement in a wallet.
 */
@Getter
@Builder
@AllArgsConstructor
public class Movement {

    private UUID id;
    private UUID walletId;
    private MovementType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceId;   // External reference (payment ID, etc.)
    private LocalDateTime createdAt;
}
