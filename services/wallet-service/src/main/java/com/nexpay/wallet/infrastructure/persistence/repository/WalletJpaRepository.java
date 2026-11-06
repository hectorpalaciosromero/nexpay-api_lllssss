package com.nexpay.wallet.infrastructure.persistence.repository;

import com.nexpay.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {

    Optional<WalletJpaEntity> findByOwnerEmail(String ownerEmail);

    boolean existsByOwnerEmail(String ownerEmail);
}
