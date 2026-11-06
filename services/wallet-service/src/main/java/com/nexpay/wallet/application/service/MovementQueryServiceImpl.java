package com.nexpay.wallet.application.service;

import com.nexpay.wallet.application.dto.MovementResponse;
import com.nexpay.wallet.domain.model.Movement;
import com.nexpay.wallet.domain.port.in.MovementQueryUseCase;
import com.nexpay.wallet.domain.port.out.MovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovementQueryServiceImpl implements MovementQueryUseCase {

    private final MovementRepository movementRepository;

    @Override
    public Page<MovementResponse> getMovements(UUID walletId, Pageable pageable) {
        log.debug("Fetching movements for wallet: {}, page: {}", walletId, pageable.getPageNumber());
        return movementRepository.findByWalletId(walletId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<MovementResponse> getMovementsByDateRange(
            UUID walletId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        log.debug("Fetching movements for wallet: {} from {} to {}", walletId, from, to);
        return movementRepository.findByWalletIdAndCreatedAtBetween(walletId, from, to, pageable)
                .map(this::toResponse);
    }

    private MovementResponse toResponse(Movement movement) {
        return MovementResponse.builder()
                .id(movement.getId())
                .walletId(movement.getWalletId())
                .type(movement.getType())
                .amount(movement.getAmount())
                .balanceAfter(movement.getBalanceAfter())
                .description(movement.getDescription())
                .referenceId(movement.getReferenceId())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
