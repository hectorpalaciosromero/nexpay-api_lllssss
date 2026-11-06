package com.nexpay.payment.api.controller;

import com.nexpay.payment.application.dto.CreatePaymentRequest;
import com.nexpay.payment.application.dto.PaymentResponse;
import com.nexpay.payment.domain.port.in.PaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "P2P payment processing API")
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    @PostMapping
    @Operation(summary = "Execute a P2P payment",
            description = "Transfers funds between wallets using Saga Pattern. " +
                    "Requires X-Idempotency-Key header to prevent duplicate payments.")
    public ResponseEntity<PaymentResponse> executePayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("X-Idempotency-Key")
            @Parameter(description = "Unique key to ensure idempotent requests")
            String idempotencyKey) {
        PaymentResponse response = paymentUseCase.executePayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID", description = "Retrieves payment details and current status")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentUseCase.getPayment(paymentId));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a payment", description = "Reverses a completed payment")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentUseCase.refundPayment(paymentId));
    }
}
