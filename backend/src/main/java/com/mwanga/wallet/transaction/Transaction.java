package com.mwanga.wallet.transaction;

import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable financial ledger entry.
 *
 * <p>Design rules:
 * <ul>
 *   <li>Rows are <b>never updated</b> after creation — append-only ledger.</li>
 *   <li>No {@code updatedAt} field; {@code BaseEntity} is intentionally not extended.</li>
 *   <li>{@code reference} is the client-supplied idempotency key; unique constraint
 *       prevents duplicate transactions from retries.</li>
 *   <li>{@code senderWallet} is {@code null} for {@link TransactionType#TOP_UP} entries.</li>
 * </ul>
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_tx_reference",        columnList = "reference"),
                @Index(name = "idx_tx_sender_wallet",    columnList = "sender_wallet_id"),
                @Index(name = "idx_tx_receiver_wallet",  columnList = "receiver_wallet_id"),
                @Index(name = "idx_tx_initiated_by",     columnList = "initiated_by"),
                @Index(name = "idx_tx_created_at",       columnList = "created_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Idempotency key — unique per transaction. */
    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /** Null for TOP_UP (admin-initiated; no sender wallet). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private Wallet receiverWallet;

    /** NUMERIC(19,4) — never float for money. */
    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Platform fee. Zero for now; retained for future monetisation. */
    @Column(nullable = false, updatable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
