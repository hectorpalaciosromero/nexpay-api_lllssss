package com.nexpay.wallet.domain.port.in;

import com.nexpay.wallet.application.dto.MovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Input port for movement queries.
 */
public interface MovementQueryUseCase {

    Page<MovementResponse> getMovements(UUID walletId, Pageable pageable);

    Page<MovementResponse> getMovementsByDateRange(
            UUID walletId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
