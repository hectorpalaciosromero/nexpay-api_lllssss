package com.nexpay.payment.application.dto;

import com.nexpay.payment.domain.model.PaymentStatus;
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
@Schema(description = "Payment information response")
public class PaymentResponse {

    private UUID id;
    private UUID senderWalletId;
    private UUID receiverWalletId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
