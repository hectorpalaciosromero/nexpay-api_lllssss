package com.nexpay.wallet.infrastructure.persistence.entity;

import com.nexpay.wallet.domain.model.MovementType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "movements", indexes = {
        @Index(name = "idx_movement_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_movement_created_at", columnList = "wallet_id, created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "wallet_id", nullable = false, columnDefinition = "uuid")
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
