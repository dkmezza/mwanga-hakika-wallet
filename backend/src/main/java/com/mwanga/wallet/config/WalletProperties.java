package com.mwanga.wallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Transfer and top-up limits from application.yml.
 * Override via env vars (e.g. APPLICATION_TRANSFER_MAX_AMOUNT) without touching code.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application")
public class WalletProperties {

    private Transfer transfer = new Transfer();
    private Topup topup = new Topup();

    @Getter
    @Setter
    public static class Transfer {
        private BigDecimal minAmount = new BigDecimal("100");
        private BigDecimal maxAmount = new BigDecimal("5000000");
    }

    @Getter
    @Setter
    public static class Topup {
        private BigDecimal minAmount = new BigDecimal("100");
        private BigDecimal maxAmount = new BigDecimal("10000000");
    }
}
