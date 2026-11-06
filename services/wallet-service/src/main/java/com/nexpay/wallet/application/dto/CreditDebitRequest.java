package com.nexpay.wallet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to credit or debit a wallet")
public class CreditDebitRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Schema(description = "Amount to credit/debit", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Description of the operation", example = "Payment from user X")
    private String description;

    @Schema(description = "External reference ID (e.g., payment ID)", example = "PAY-abc123")
    private String referenceId;
}
