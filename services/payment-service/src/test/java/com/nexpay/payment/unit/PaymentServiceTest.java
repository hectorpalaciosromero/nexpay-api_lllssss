package com.nexpay.payment.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexpay.payment.application.dto.CreatePaymentRequest;
import com.nexpay.payment.application.service.PaymentServiceImpl;
import com.nexpay.payment.domain.model.Payment;
import com.nexpay.payment.domain.model.PaymentStatus;
import com.nexpay.payment.domain.exception.PaymentNotFoundException;
import com.nexpay.payment.domain.port.out.PaymentRepository;
import com.nexpay.payment.infrastructure.client.WalletServiceClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletServiceClient walletClient;

    @Mock
    private SqsClient sqsClient;

    private PaymentServiceImpl paymentService;

    private UUID senderWalletId;
    private UUID receiverWalletId;

    @BeforeEach
    void setUp() {
        senderWalletId = UUID.randomUUID();
        receiverWalletId = UUID.randomUUID();

        paymentService = new PaymentServiceImpl(
                paymentRepository,
                walletClient,
                sqsClient,
                new ObjectMapper(),
                "http://localhost:4566/000000000000/payment-events",
                new SimpleMeterRegistry()
        );
    }

    @Nested
    @DisplayName("Create Payment - Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should reject payment with negative amount")
        void shouldRejectNegativeAmount() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("-50.00"))
                    .description("Invalid payment")
                    .build();

            assertThatThrownBy(() -> paymentService.executePayment(request, UUID.randomUUID().toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("Should reject payment with zero amount")
        void shouldRejectZeroAmount() {
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(BigDecimal.ZERO)
                    .description("Zero payment")
                    .build();

            assertThatThrownBy(() -> paymentService.executePayment(request, UUID.randomUUID().toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        @DisplayName("Should reject self-transfer")
        void shouldRejectSelfTransfer() {
            UUID sameWallet = UUID.randomUUID();
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .senderWalletId(sameWallet)
                    .receiverWalletId(sameWallet)
                    .amount(new BigDecimal("100.00"))
                    .description("Self transfer")
                    .build();

            assertThatThrownBy(() -> paymentService.executePayment(request, UUID.randomUUID().toString()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("same wallet");
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should return existing payment for duplicate idempotency key")
        void shouldReturnExistingForDuplicateKey() {
            String idempotencyKey = "unique-key-123";
            UUID existingId = UUID.randomUUID();
            Payment existingPayment = Payment.builder()
                    .id(existingId)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(PaymentStatus.COMPLETED)
                    .idempotencyKey(idempotencyKey)
                    .build();

            when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingPayment));

            // Act — call the method under test
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("100.00"))
                    .description("Duplicate payment")
                    .build();

            var result = paymentService.executePayment(request, idempotencyKey);

            // Assert — should return existing, NOT create a new one
            assertThat(result.getId()).isEqualTo(existingId);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

            // Verify wallet service was never called (no saga triggered)
            verify(walletClient, never()).debitWallet(any(), any(), any(), any());
            verify(walletClient, never()).creditWallet(any(), any(), any(), any());
            // Verify no new payment was saved
            verify(paymentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Payment")
    class GetPaymentTests {

        @Test
        @DisplayName("Should return payment by ID")
        void shouldReturnPaymentById() {
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .id(paymentId)
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .amount(new BigDecimal("250.00"))
                    .status(PaymentStatus.COMPLETED)
                    .build();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            var result = paymentService.getPayment(paymentId);

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should throw for non-existent payment")
        void shouldThrowForNonExistent() {
            UUID nonExistentId = UUID.randomUUID();
            when(paymentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPayment(nonExistentId))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Payment Domain Model")
    class PaymentDomainTests {

        @Test
        @DisplayName("Payment status transitions should be valid")
        void shouldValidateStatusTransitions() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .status(PaymentStatus.PENDING)
                    .amount(new BigDecimal("100.00"))
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .build();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

            payment.startProcessing();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

            payment.complete();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should fail on invalid status transition")
        void shouldFailOnInvalidTransition() {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .status(PaymentStatus.PENDING)
                    .amount(new BigDecimal("100.00"))
                    .senderWalletId(senderWalletId)
                    .receiverWalletId(receiverWalletId)
                    .build();

            assertThatThrownBy(payment::complete)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("All PaymentStatus values should be defined")
        void shouldHaveAllStatusValues() {
            assertThat(PaymentStatus.values()).containsExactlyInAnyOrder(
                    PaymentStatus.PENDING,
                    PaymentStatus.PROCESSING,
                    PaymentStatus.COMPLETED,
                    PaymentStatus.FAILED,
                    PaymentStatus.REFUND_PENDING,
                    PaymentStatus.REFUNDED
            );
        }
    }
}
