package com.mwanga.wallet.wallet.dto;

import com.mwanga.wallet.wallet.Wallet;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WalletResponse {

    private final UUID id;
    private final UUID userId;
    private final String userFullName;
    private final BigDecimal balance;
    private final String currency;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static WalletResponse from(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .userFullName(wallet.getUser().getFullName())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .active(wallet.isActive())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
