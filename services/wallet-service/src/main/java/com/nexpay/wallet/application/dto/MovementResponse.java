package com.nexpay.wallet.application.dto;

import com.nexpay.wallet.domain.model.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Movement (transaction) information response")
public class MovementResponse {

    @Schema(description = "Movement unique identifier")
    private UUID id;

    @Schema(description = "Wallet ID this movement belongs to")
    private UUID walletId;

    @Schema(description = "Type of movement")
    private MovementType type;

    @Schema(description = "Movement amount")
    private BigDecimal amount;

    @Schema(description = "Balance after this movement")
    private BigDecimal balanceAfter;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "External reference ID")
    private String referenceId;

    @Schema(description = "Movement timestamp")
    private LocalDateTime createdAt;
}
