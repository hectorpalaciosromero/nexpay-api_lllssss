package com.nexpay.wallet.domain.port.in;

import com.nexpay.wallet.application.dto.CreateWalletRequest;
import com.nexpay.wallet.application.dto.WalletResponse;
import com.nexpay.wallet.application.dto.CreditDebitRequest;
import com.nexpay.wallet.domain.model.WalletStatus;

import java.util.UUID;

/**
 * Input port for wallet operations.
 */
public interface WalletUseCase {

    WalletResponse createWallet(CreateWalletRequest request);

    WalletResponse getWallet(UUID walletId);

    WalletResponse getWalletByEmail(String email);

    WalletResponse creditWallet(UUID walletId, CreditDebitRequest request);

    WalletResponse debitWallet(UUID walletId, CreditDebitRequest request);

    WalletResponse updateStatus(UUID walletId, WalletStatus status);
}
