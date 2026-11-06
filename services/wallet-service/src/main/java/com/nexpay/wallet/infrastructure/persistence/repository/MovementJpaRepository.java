package com.nexpay.wallet.infrastructure.persistence.repository;

import com.nexpay.wallet.infrastructure.persistence.entity.MovementJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface MovementJpaRepository extends JpaRepository<MovementJpaEntity, UUID> {

    Page<MovementJpaEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    Page<MovementJpaEntity> findByWalletIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID walletId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
