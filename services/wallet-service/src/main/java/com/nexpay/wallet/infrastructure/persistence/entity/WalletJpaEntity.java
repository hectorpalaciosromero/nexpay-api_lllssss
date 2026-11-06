package com.nexpay.wallet.infrastructure.persistence.entity;

import com.nexpay.wallet.domain.model.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for Wallet. Uses optimistic locking via @Version for concurrent balance updates.
 */
@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_email", columnList = "owner_email", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_email", nullable = false, unique = true, length = 255)
    private String ownerEmail;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
