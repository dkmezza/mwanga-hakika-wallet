package com.mwanga.wallet.wallet;

import com.mwanga.wallet.common.BaseEntity;
import com.mwanga.wallet.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wallet entity — the financial account for a user.
 *
 * <p>Key decisions:
 * <ul>
 *   <li>{@code balance} uses {@code NUMERIC(19,4)} — never float/double for money.</li>
 *   <li>{@code version} enables JPA optimistic locking to prevent concurrent
 *       double-spend without explicit row-level locks.</li>
 *   <li>One-to-one with User; a wallet is created automatically on registration.</li>
 * </ul>
 */
@Entity
@Table(
        name = "wallets",
        indexes = @Index(name = "idx_wallets_user_id", columnList = "user_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** NUMERIC(19,4) — sufficient for large TZS amounts with sub-cent precision. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /** ISO 4217 currency code. Defaults to TZS (Tanzanian Shilling). */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Optimistic lock version. Hibernate increments this on every UPDATE.
     * If two concurrent transactions read version=5 and both try to write,
     * one will succeed; the other throws {@link jakarta.persistence.OptimisticLockException}.
     */
    @Version
    private Long version;
}
