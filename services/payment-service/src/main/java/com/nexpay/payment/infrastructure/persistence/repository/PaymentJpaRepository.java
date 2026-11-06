package com.nexpay.payment.infrastructure.persistence.repository;

import com.nexpay.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
