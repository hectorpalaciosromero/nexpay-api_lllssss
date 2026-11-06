package com.nexpay.wallet.infrastructure.persistence.adapter;

import com.nexpay.wallet.domain.model.Movement;
import com.nexpay.wallet.domain.port.out.MovementRepository;
import com.nexpay.wallet.infrastructure.persistence.entity.MovementJpaEntity;
import com.nexpay.wallet.infrastructure.persistence.repository.MovementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MovementPersistenceAdapter implements MovementRepository {

    private final MovementJpaRepository jpaRepository;

    @Override
    public Movement save(Movement movement) {
        MovementJpaEntity entity = toEntity(movement);
        MovementJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<Movement> findByWalletId(UUID walletId, Pageable pageable) {
        return jpaRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(this::toDomain);
    }

    @Override
    public Page<Movement> findByWalletIdAndCreatedAtBetween(
            UUID walletId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return jpaRepository
                .findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(walletId, from, to, pageable)
                .map(this::toDomain);
    }

    private MovementJpaEntity toEntity(Movement m) {
        return MovementJpaEntity.builder()
                .id(m.getId())
                .walletId(m.getWalletId())
                .type(m.getType())
                .amount(m.getAmount())
                .balanceAfter(m.getBalanceAfter())
                .description(m.getDescription())
                .referenceId(m.getReferenceId())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private Movement toDomain(MovementJpaEntity e) {
        return Movement.builder()
                .id(e.getId())
                .walletId(e.getWalletId())
                .type(e.getType())
                .amount(e.getAmount())
                .balanceAfter(e.getBalanceAfter())
                .description(e.getDescription())
                .referenceId(e.getReferenceId())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
