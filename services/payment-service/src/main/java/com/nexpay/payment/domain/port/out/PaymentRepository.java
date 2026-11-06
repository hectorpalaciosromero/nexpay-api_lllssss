package com.nexpay.payment.domain.port.out;

import com.nexpay.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
