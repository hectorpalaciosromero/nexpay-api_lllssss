package com.nexpay.wallet.unit;

import com.nexpay.wallet.domain.model.Wallet;
import com.nexpay.wallet.domain.model.WalletStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Wallet domain model.
 */
@DisplayName("Wallet Domain Model")
class WalletDomainTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerEmail("test@nexpay.com")
                .ownerName("Test User")
                .currency("USD")
                .balance(new BigDecimal("1000.00"))
                .status(WalletStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Credit operations")
    class CreditTests {

        @Test
        @DisplayName("Should credit wallet successfully")
        void shouldCreditWallet() {
            wallet.credit(new BigDecimal("250.00"));
            assertEquals(new BigDecimal("1250.00"), wallet.getBalance());
        }

        @Test
        @DisplayName("Should reject credit with zero amount")
        void shouldRejectZeroCredit() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.credit(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should reject credit with negative amount")
        void shouldRejectNegativeCredit() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.credit(new BigDecimal("-50.00")));
        }

        @Test
        @DisplayName("Should reject credit on inactive wallet")
        void shouldRejectCreditOnInactiveWallet() {
            wallet.deactivate();
            assertThrows(IllegalStateException.class,
                    () -> wallet.credit(new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Debit operations")
    class DebitTests {

        @Test
        @DisplayName("Should debit wallet successfully")
        void shouldDebitWallet() {
            wallet.debit(new BigDecimal("300.00"));
            assertEquals(new BigDecimal("700.00"), wallet.getBalance());
        }

        @Test
        @DisplayName("Should reject debit exceeding balance")
        void shouldRejectInsufficientBalance() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.debit(new BigDecimal("1500.00")));
        }

        @Test
        @DisplayName("Should allow debit of exact balance")
        void shouldAllowExactBalanceDebit() {
            wallet.debit(new BigDecimal("1000.00"));
            assertEquals(BigDecimal.ZERO.setScale(2), wallet.getBalance().setScale(2));
        }

        @Test
        @DisplayName("Should reject debit on blocked wallet")
        void shouldRejectDebitOnBlockedWallet() {
            wallet.block();
            assertThrows(IllegalStateException.class,
                    () -> wallet.debit(new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTests {

        @Test
        @DisplayName("Should activate wallet")
        void shouldActivate() {
            wallet.deactivate();
            wallet.activate();
            assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        }

        @Test
        @DisplayName("Should block wallet")
        void shouldBlock() {
            wallet.block();
            assertEquals(WalletStatus.BLOCKED, wallet.getStatus());
        }

        @Test
        @DisplayName("Should close wallet with zero balance")
        void shouldCloseWithZeroBalance() {
            wallet.debit(new BigDecimal("1000.00"));
            wallet.close();
            assertEquals(WalletStatus.CLOSED, wallet.getStatus());
        }

        @Test
        @DisplayName("Should reject closing wallet with balance")
        void shouldRejectCloseWithBalance() {
            assertThrows(IllegalStateException.class, () -> wallet.close());
        }

        @Test
        @DisplayName("Should reject activating closed wallet")
        void shouldRejectActivatingClosedWallet() {
            wallet.debit(new BigDecimal("1000.00"));
            wallet.close();
            assertThrows(IllegalStateException.class, () -> wallet.activate());
        }
    }
}
