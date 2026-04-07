package com.mwanga.wallet.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Peer-to-peer fund transfer request")
public class TransferRequest {

    @NotNull(message = "Receiver wallet ID is required")
    @Schema(description = "Destination wallet ID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID receiverWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100", message = "Minimum transfer amount is 100 TZS")
    @DecimalMax(value = "5000000", message = "Maximum transfer amount is 5,000,000 TZS")
    @Schema(description = "Amount to transfer (TZS)", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Optional transfer note", example = "Lunch split")
    private String description;

    /**
     * Client-supplied idempotency key (UUID recommended).
     * Supply the same key on a retry to avoid double-charging.
     * If omitted, a random server-generated key is used.
     */
    @Schema(description = "Optional idempotency key for safe retries",
            example = "550e8400-e29b-41d4-a716-446655440001")
    private String idempotencyKey;
}
