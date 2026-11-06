package com.nexpay.payment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a P2P payment")
public class CreatePaymentRequest {

    @NotNull(message = "Sender wallet ID is required")
    @Schema(description = "Sender's wallet UUID")
    private UUID senderWalletId;

    @NotNull(message = "Receiver wallet ID is required")
    @Schema(description = "Receiver's wallet UUID")
    private UUID receiverWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Schema(description = "Payment amount", example = "50.00")
    private BigDecimal amount;

    @Schema(description = "Payment description", example = "Dinner split")
    private String description;
}
