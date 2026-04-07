package com.mwanga.wallet.requisition.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mwanga.wallet.requisition.RequisitionStatus;
import com.mwanga.wallet.requisition.TopUpRequisition;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionResponse {

    private final UUID id;
    private final UUID userId;
    private final String userFullName;
    private final UUID walletId;
    private final BigDecimal requestedAmount;
    private final RequisitionStatus status;
    private final String note;

    /** Populated after admin reviews the request. */
    private final String adminNote;
    private final UUID reviewedById;
    private final Instant reviewedAt;

    private final Instant createdAt;
    private final Instant updatedAt;

    public static RequisitionResponse from(TopUpRequisition r) {
        return RequisitionResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userFullName(r.getUser().getFullName())
                .walletId(r.getWallet().getId())
                .requestedAmount(r.getRequestedAmount())
                .status(r.getStatus())
                .note(r.getNote())
                .adminNote(r.getAdminNote())
                .reviewedById(r.getReviewedBy() != null ? r.getReviewedBy().getId() : null)
                .reviewedAt(r.getReviewedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
