package com.nexpay.wallet.application.service;

import com.nexpay.wallet.application.dto.*;
import com.nexpay.wallet.domain.exception.WalletAlreadyExistsException;
import com.nexpay.wallet.domain.exception.WalletNotFoundException;
import com.nexpay.wallet.domain.model.*;
import com.nexpay.wallet.domain.port.in.WalletUseCase;
import com.nexpay.wallet.domain.port.out.MovementRepository;
import com.nexpay.wallet.domain.port.out.WalletRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application service implementing wallet use cases.
 */
@Slf4j
@Service
@Transactional
public class WalletServiceImpl implements WalletUseCase {

    private final WalletRepository walletRepository;
    private final MovementRepository movementRepository;
    private final Counter walletCreatedCounter;
    private final Counter transactionCounter;
    private final Timer transactionTimer;

    public WalletServiceImpl(
            WalletRepository walletRepository,
            MovementRepository movementRepository,
            MeterRegistry meterRegistry) {
        this.walletRepository = walletRepository;
        this.movementRepository = movementRepository;

        // Custom business metrics for Grafana dashboards
        this.walletCreatedCounter = Counter.builder("nexpay.wallets.created")
                .description("Total wallets created")
                .register(meterRegistry);
        this.transactionCounter = Counter.builder("nexpay.transactions.total")
                .description("Total wallet transactions")
                .tag("type", "all")
                .register(meterRegistry);
        this.transactionTimer = Timer.builder("nexpay.transactions.duration")
                .description("Transaction processing time")
                .register(meterRegistry);
    }

    @Override
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating wallet for email: {}", request.getOwnerEmail());

        if (walletRepository.existsByOwnerEmail(request.getOwnerEmail())) {
            throw new WalletAlreadyExistsException(request.getOwnerEmail());
        }

        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerEmail(request.getOwnerEmail())
                .ownerName(request.getOwnerName())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(wallet);
        walletCreatedCounter.increment();

        log.info("Wallet created successfully: {}", saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "wallets", key = "#walletId")
    public WalletResponse getWallet(UUID walletId) {
        log.debug("Fetching wallet: {}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> WalletNotFoundException.byId(walletId));
        return toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByEmail(String email) {
        log.debug("Fetching wallet by email: {}", email);
        Wallet wallet = walletRepository.findByOwnerEmail(email)
                .orElseThrow(() -> WalletNotFoundException.byEmail(email));
        return toResponse(wallet);
    }

    @Override
    @CacheEvict(value = "wallets", key = "#walletId")
    public WalletResponse creditWallet(UUID walletId, CreditDebitRequest request) {
        return transactionTimer.record(() -> {
            log.info("Crediting wallet {} with amount {}", walletId, request.getAmount());

            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> WalletNotFoundException.byId(walletId));

            wallet.credit(request.getAmount());
            Wallet saved = walletRepository.save(wallet);

            // Record movement
            Movement movement = Movement.builder()
                    .id(UUID.randomUUID())
                    .walletId(walletId)
                    .type(MovementType.CREDIT)
                    .amount(request.getAmount())
                    .balanceAfter(saved.getBalance())
                    .description(request.getDescription())
                    .referenceId(request.getReferenceId())
                    .build();
            movementRepository.save(movement);

            transactionCounter.increment();
            log.info("Wallet {} credited. New balance: {}", walletId, saved.getBalance());
            return toResponse(saved);
        });
    }

    @Override
    @CacheEvict(value = "wallets", key = "#walletId")
    public WalletResponse debitWallet(UUID walletId, CreditDebitRequest request) {
        return transactionTimer.record(() -> {
            log.info("Debiting wallet {} with amount {}", walletId, request.getAmount());

            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> WalletNotFoundException.byId(walletId));

            wallet.debit(request.getAmount());
            Wallet saved = walletRepository.save(wallet);

            // Record movement
            Movement movement = Movement.builder()
                    .id(UUID.randomUUID())
                    .walletId(walletId)
                    .type(MovementType.DEBIT)
                    .amount(request.getAmount())
                    .balanceAfter(saved.getBalance())
                    .description(request.getDescription())
                    .referenceId(request.getReferenceId())
                    .build();
            movementRepository.save(movement);

            transactionCounter.increment();
            log.info("Wallet {} debited. New balance: {}", walletId, saved.getBalance());
            return toResponse(saved);
        });
    }

    @Override
    @CacheEvict(value = "wallets", key = "#walletId")
    public WalletResponse updateStatus(UUID walletId, WalletStatus status) {
        log.info("Updating wallet {} status to {}", walletId, status);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> WalletNotFoundException.byId(walletId));

        switch (status) {
            case ACTIVE -> wallet.activate();
            case INACTIVE -> wallet.deactivate();
            case BLOCKED -> wallet.block();
            case CLOSED -> wallet.close();
        }

        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet {} status updated to {}", walletId, status);
        return toResponse(saved);
    }

    private WalletResponse toResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .ownerEmail(wallet.getOwnerEmail())
                .ownerName(wallet.getOwnerName())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
