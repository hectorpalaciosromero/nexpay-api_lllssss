package com.nexpay.payment.infrastructure.persistence.adapter;

import com.nexpay.payment.domain.model.Payment;
import com.nexpay.payment.domain.port.out.PaymentRepository;
import com.nexpay.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.nexpay.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = toEntity(payment);
        PaymentJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    private PaymentJpaEntity toEntity(Payment p) {
        return PaymentJpaEntity.builder()
                .id(p.getId())
                .idempotencyKey(p.getIdempotencyKey())
                .senderWalletId(p.getSenderWalletId())
                .receiverWalletId(p.getReceiverWalletId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .status(p.getStatus())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }

    private Payment toDomain(PaymentJpaEntity e) {
        return Payment.builder()
                .id(e.getId())
                .idempotencyKey(e.getIdempotencyKey())
                .senderWalletId(e.getSenderWalletId())
                .receiverWalletId(e.getReceiverWalletId())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .description(e.getDescription())
                .status(e.getStatus())
                .failureReason(e.getFailureReason())
                .createdAt(e.getCreatedAt())
                .completedAt(e.getCompletedAt())
                .build();
    }
}
