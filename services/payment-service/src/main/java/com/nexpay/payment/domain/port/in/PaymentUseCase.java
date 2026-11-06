package com.nexpay.payment.domain.port.in;

import com.nexpay.payment.application.dto.CreatePaymentRequest;
import com.nexpay.payment.application.dto.PaymentResponse;

import java.util.UUID;

public interface PaymentUseCase {

    PaymentResponse executePayment(CreatePaymentRequest request, String idempotencyKey);

    PaymentResponse getPayment(UUID paymentId);

    PaymentResponse refundPayment(UUID paymentId);
}
