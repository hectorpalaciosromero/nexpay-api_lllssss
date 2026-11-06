package com.nexpay.wallet.application.dto;

import com.nexpay.wallet.domain.model.WalletStatus;
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
@Schema(description = "Wallet information response")
public class WalletResponse {

    @Schema(description = "Wallet unique identifier")
    private UUID id;

    @Schema(description = "Owner's email address")
    private String ownerEmail;

    @Schema(description = "Owner's full name")
    private String ownerName;

    @Schema(description = "Wallet currency")
    private String currency;

    @Schema(description = "Current balance")
    private BigDecimal balance;

    @Schema(description = "Wallet status")
    private WalletStatus status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
