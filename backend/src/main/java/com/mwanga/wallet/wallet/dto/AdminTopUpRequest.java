package com.mwanga.wallet.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Admin wallet top-up request")
public class AdminTopUpRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100", message = "Minimum top-up amount is 100 TZS")
    @DecimalMax(value = "10000000", message = "Maximum top-up amount is 10,000,000 TZS")
    @Schema(description = "Amount to credit (TZS)", example = "50000.00")
    private BigDecimal amount;

    @Schema(description = "Reason or note for this top-up", example = "Initial wallet funding")
    private String description;

    /**
     * Client-supplied idempotency key (UUID recommended).
     * If provided and a transaction with this key already exists, the existing
     * transaction is returned instead of creating a duplicate.
     * If omitted, a server-generated key is used (retries will not be deduplicated).
     */
    @Schema(description = "Optional idempotency key — supply a UUID to make retries safe",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String idempotencyKey;
}
