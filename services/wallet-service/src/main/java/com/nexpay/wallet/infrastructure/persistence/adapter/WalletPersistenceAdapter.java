package com.nexpay.wallet.infrastructure.persistence.adapter;

import com.nexpay.wallet.domain.model.Wallet;
import com.nexpay.wallet.domain.port.out.WalletRepository;
import com.nexpay.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.nexpay.wallet.infrastructure.persistence.repository.WalletJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the WalletRepository port.
 * Translates between domain model and JPA entity.
 */
@Component
@RequiredArgsConstructor
public class WalletPersistenceAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = toEntity(wallet);
        WalletJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findByOwnerEmail(String email) {
        return jpaRepository.findByOwnerEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByOwnerEmail(String email) {
        return jpaRepository.existsByOwnerEmail(email);
    }

    // Mapping methods

    private WalletJpaEntity toEntity(Wallet wallet) {
        return WalletJpaEntity.builder()
                .id(wallet.getId())
                .ownerEmail(wallet.getOwnerEmail())
                .ownerName(wallet.getOwnerName())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .version(wallet.getVersion())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private Wallet toDomain(WalletJpaEntity entity) {
        return Wallet.builder()
                .id(entity.getId())
                .ownerEmail(entity.getOwnerEmail())
                .ownerName(entity.getOwnerName())
                .currency(entity.getCurrency())
                .balance(entity.getBalance())
                .status(entity.getStatus())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
