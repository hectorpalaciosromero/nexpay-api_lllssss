package com.nexpay.wallet.api.controller;

import com.nexpay.wallet.application.dto.*;
import com.nexpay.wallet.domain.model.WalletStatus;
import com.nexpay.wallet.domain.port.in.MovementQueryUseCase;
import com.nexpay.wallet.domain.port.in.WalletUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Digital wallet management API")
public class WalletController {

    private final WalletUseCase walletUseCase;
    private final MovementQueryUseCase movementQueryUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new wallet", description = "Creates a new digital wallet for a user")
    @ApiResponse(responseCode = "201", description = "Wallet created successfully")
    @ApiResponse(responseCode = "409", description = "Wallet already exists for this email")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        WalletResponse response = walletUseCase.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet by ID", description = "Retrieves wallet information including current balance")
    @ApiResponse(responseCode = "200", description = "Wallet found")
    @ApiResponse(responseCode = "404", description = "Wallet not found")
    public ResponseEntity<WalletResponse> getWallet(
            @PathVariable @Parameter(description = "Wallet UUID") UUID walletId) {
        return ResponseEntity.ok(walletUseCase.getWallet(walletId));
    }

    @GetMapping("/by-email")
    @Operation(summary = "Get wallet by email", description = "Retrieves wallet by owner's email address")
    public ResponseEntity<WalletResponse> getWalletByEmail(
            @RequestParam @Parameter(description = "Owner email") String email) {
        return ResponseEntity.ok(walletUseCase.getWalletByEmail(email));
    }

    @PostMapping("/{walletId}/credit")
    @Operation(summary = "Credit wallet", description = "Adds funds to the wallet")
    @ApiResponse(responseCode = "200", description = "Wallet credited successfully")
    @ApiResponse(responseCode = "400", description = "Invalid amount or wallet inactive")
    public ResponseEntity<WalletResponse> creditWallet(
            @PathVariable UUID walletId,
            @Valid @RequestBody CreditDebitRequest request) {
        return ResponseEntity.ok(walletUseCase.creditWallet(walletId, request));
    }

    @PostMapping("/{walletId}/debit")
    @Operation(summary = "Debit wallet", description = "Withdraws funds from the wallet")
    @ApiResponse(responseCode = "200", description = "Wallet debited successfully")
    @ApiResponse(responseCode = "400", description = "Insufficient balance or wallet inactive")
    public ResponseEntity<WalletResponse> debitWallet(
            @PathVariable UUID walletId,
            @Valid @RequestBody CreditDebitRequest request) {
        return ResponseEntity.ok(walletUseCase.debitWallet(walletId, request));
    }

    @PatchMapping("/{walletId}/status")
    @Operation(summary = "Update wallet status", description = "Changes wallet status (ACTIVE, INACTIVE, BLOCKED, CLOSED)")
    public ResponseEntity<WalletResponse> updateStatus(
            @PathVariable UUID walletId,
            @RequestParam WalletStatus status) {
        return ResponseEntity.ok(walletUseCase.updateStatus(walletId, status));
    }

    @GetMapping("/{walletId}/movements")
    @Operation(summary = "Get wallet movements", description = "Retrieves paginated movement history for a wallet")
    public ResponseEntity<Page<MovementResponse>> getMovements(
            @PathVariable UUID walletId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(movementQueryUseCase.getMovements(walletId, pageable));
    }

    @GetMapping("/{walletId}/movements/range")
    @Operation(summary = "Get movements by date range", description = "Retrieves movements within a date range")
    public ResponseEntity<Page<MovementResponse>> getMovementsByDateRange(
            @PathVariable UUID walletId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movementQueryUseCase.getMovementsByDateRange(walletId, from, to, pageable));
    }
}
