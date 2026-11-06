package com.nexpay.wallet.domain.port.out;

import com.nexpay.wallet.domain.model.Wallet;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for wallet persistence.
 */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByOwnerEmail(String email);

    boolean existsByOwnerEmail(String email);
}
