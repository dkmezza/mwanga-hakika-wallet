package com.mwanga.wallet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so that {@code @CreatedDate} and {@code @LastModifiedDate}
 * in {@link com.mwanga.wallet.common.BaseEntity} are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class AuditConfig {
}
