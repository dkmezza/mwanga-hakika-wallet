package com.mwanga.wallet.requisition;

import com.mwanga.wallet.common.BaseEntity;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A user's request to have their wallet topped up by an admin.
 *
 * <p>Lifecycle: PENDING → APPROVED (wallet credited) | REJECTED.
 * Unlike {@link com.mwanga.wallet.transaction.Transaction}, this entity IS mutable
 * (status changes), so it extends {@link BaseEntity} for audit timestamps.
 */
@Entity
@Table(
        name = "top_up_requisitions",
        indexes = {
                @Index(name = "idx_req_user_id",  columnList = "user_id"),
                @Index(name = "idx_req_status",   columnList = "status"),
                @Index(name = "idx_req_created",  columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequisition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RequisitionStatus status = RequisitionStatus.PENDING;

    /** User's optional note (e.g. payment reference). */
    @Column(length = 500)
    private String note;

    /** Admin's decision note (rejection reason, approval comment). */
    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
