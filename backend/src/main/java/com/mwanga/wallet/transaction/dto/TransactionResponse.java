package com.mwanga.wallet.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mwanga.wallet.transaction.Transaction;
import com.mwanga.wallet.transaction.TransactionStatus;
import com.mwanga.wallet.transaction.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private final UUID id;
    private final String reference;
    private final TransactionType type;
    private final TransactionStatus status;

    /** Null for TOP_UP transactions (no sender). */
    private final UUID senderWalletId;
    private final UUID receiverWalletId;

    private final BigDecimal amount;
    private final BigDecimal fee;
    private final String description;
    private final UUID initiatedById;
    private final Instant createdAt;

    public static TransactionResponse from(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .reference(tx.getReference())
                .type(tx.getType())
                .status(tx.getStatus())
                .senderWalletId(tx.getSenderWallet() != null ? tx.getSenderWallet().getId() : null)
                .receiverWalletId(tx.getReceiverWallet().getId())
                .amount(tx.getAmount())
                .fee(tx.getFee())
                .description(tx.getDescription())
                .initiatedById(tx.getInitiatedBy().getId())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
