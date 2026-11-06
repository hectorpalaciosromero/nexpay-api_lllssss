package com.nexpay.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexpay.payment.application.dto.CreatePaymentRequest;
import com.nexpay.payment.application.dto.PaymentResponse;
import com.nexpay.payment.domain.model.Payment;
import com.nexpay.payment.domain.model.PaymentEvent;
import com.nexpay.payment.domain.model.PaymentStatus;
import com.nexpay.payment.domain.exception.PaymentNotFoundException;
import com.nexpay.payment.domain.port.in.PaymentUseCase;
import com.nexpay.payment.domain.port.out.PaymentRepository;
import com.nexpay.payment.infrastructure.client.WalletServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment service handling the saga flow for P2P transfers.
 *
 * Flow:
 * 1. Create payment (PENDING)
 * 2. Debit sender wallet (PROCESSING) via wallet-service
 * 3. Credit receiver wallet via wallet-service
 * 4. If step 3 fails, compensate by refunding sender
 * 5. Mark as COMPLETED and publish event to SQS
 */
@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final WalletServiceClient walletClient;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String paymentEventsQueueUrl;

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Timer paymentTimer;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            WalletServiceClient walletClient,
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.payment-events-url}") String paymentEventsQueueUrl,
            MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.walletClient = walletClient;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.paymentEventsQueueUrl = paymentEventsQueueUrl;

        this.paymentSuccessCounter = Counter.builder("nexpay.payments.success")
                .description("Successful payments").register(meterRegistry);
        this.paymentFailedCounter = Counter.builder("nexpay.payments.failed")
                .description("Failed payments").register(meterRegistry);
        this.paymentTimer = Timer.builder("nexpay.payments.duration")
                .description("Payment processing time").register(meterRegistry);
    }

    @Override
    @CircuitBreaker(name = "walletService", fallbackMethod = "paymentFallback")
    @Retry(name = "walletService")
    public PaymentResponse executePayment(CreatePaymentRequest request, String idempotencyKey) {
        return paymentTimer.record(() -> {
            log.info("Executing payment: sender={}, receiver={}, amount={}, idempotencyKey={}",
                    request.getSenderWalletId(), request.getReceiverWalletId(),
                    request.getAmount(), idempotencyKey);

            // Idempotency check: if payment with this key exists, return it
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent request detected, returning existing payment: {}", existing.get().getId());
                return toResponse(existing.get());
            }

            // Validate amount
            if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }

            // Validate sender != receiver
            if (request.getSenderWalletId().equals(request.getReceiverWalletId())) {
                throw new IllegalArgumentException("Cannot send payment to the same wallet");
            }

            // Step 1: Create payment record
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .senderWalletId(request.getSenderWalletId())
                    .receiverWalletId(request.getReceiverWalletId())
                    .amount(request.getAmount())
                    .currency("USD")
                    .description(request.getDescription())
                    .status(PaymentStatus.PENDING)
                    .build();

            payment = paymentRepository.save(payment);

            // Transition to PROCESSING before the saga begins
            payment.startProcessing();
            payment = paymentRepository.save(payment);

            try {
                // Step 2 (Saga): Debit sender
                walletClient.debitWallet(request.getSenderWalletId(),
                        request.getAmount(), "Payment to " + request.getReceiverWalletId(),
                        payment.getId().toString());

                // Step 3 (Saga): Credit receiver
                try {
                    walletClient.creditWallet(request.getReceiverWalletId(),
                            request.getAmount(), "Payment from " + request.getSenderWalletId(),
                            payment.getId().toString());
                } catch (Exception e) {
                    // COMPENSATION: Refund the sender since credit failed
                    log.error("Credit failed, compensating sender wallet: {}", e.getMessage());
                    walletClient.creditWallet(request.getSenderWalletId(),
                            request.getAmount(), "Refund - failed payment " + payment.getId(),
                            "REFUND-" + payment.getId());
                    throw e;
                }

                // Step 4: Mark completed
                payment.complete();
                payment = paymentRepository.save(payment);
                paymentSuccessCounter.increment();

                // Step 5: Publish event to SQS
                publishPaymentEvent(payment, "PAYMENT_COMPLETED");

                log.info("Payment completed successfully: {}", payment.getId());
                return toResponse(payment);

            } catch (Exception e) {
                payment.fail(e.getMessage());
                paymentRepository.save(payment);
                paymentFailedCounter.increment();

                publishPaymentEvent(payment, "PAYMENT_FAILED");

                log.error("Payment failed: {}", e.getMessage());
                throw new RuntimeException("Payment failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));
        return toResponse(payment);
    }

    @Override
    public PaymentResponse refundPayment(UUID paymentId) {
        log.info("Refunding payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.toString()));

        payment.initiateRefund();

        // Reverse: debit receiver, credit sender
        walletClient.debitWallet(payment.getReceiverWalletId(),
                payment.getAmount(), "Refund for payment " + paymentId,
                "REFUND-" + paymentId);
        walletClient.creditWallet(payment.getSenderWalletId(),
                payment.getAmount(), "Refund received for payment " + paymentId,
                "REFUND-" + paymentId);

        payment.refund();
        payment = paymentRepository.save(payment);

        publishPaymentEvent(payment, "PAYMENT_REFUNDED");

        log.info("Payment refunded: {}", paymentId);
        return toResponse(payment);
    }

    /**
     * Circuit breaker fallback when wallet-service is down.
     */
    @SuppressWarnings("unused")
    private PaymentResponse paymentFallback(CreatePaymentRequest request, String idempotencyKey, Exception ex) {
        log.warn("Circuit breaker triggered for payment. Wallet service unavailable: {}", ex.getMessage());
        throw new RuntimeException("Payment service temporarily unavailable. Please try again later.");
    }

    private void publishPaymentEvent(Payment payment, String eventType) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                    .eventType(eventType)
                    .paymentId(payment.getId())
                    .senderWalletId(payment.getSenderWalletId())
                    .receiverWalletId(payment.getReceiverWalletId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .description(payment.getDescription())
                    .failureReason(payment.getFailureReason())
                    .timestamp(Instant.now())
                    .build();

            String messageBody = objectMapper.writeValueAsString(event);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(paymentEventsQueueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(payment.getSenderWalletId().toString())
                    .build());

            log.info("Published {} event to SQS for payment: {}", eventType, payment.getId());
        } catch (Exception e) {
            log.error("Failed to publish event to SQS: {}", e.getMessage());
            // Don't fail the payment if event publishing fails - eventual consistency
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
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
}
