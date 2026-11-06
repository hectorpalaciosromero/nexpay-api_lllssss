package com.nexpay.wallet.domain.port.out;

import com.nexpay.wallet.domain.model.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Output port for Movement persistence.
 */
public interface MovementRepository {

    Movement save(Movement movement);

    Page<Movement> findByWalletId(UUID walletId, Pageable pageable);

    Page<Movement> findByWalletIdAndCreatedAtBetween(
            UUID walletId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
