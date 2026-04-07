package com.mwanga.wallet.requisition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "User top-up request submission")
public class RequisitionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100", message = "Minimum request amount is 100 TZS")
    @DecimalMax(value = "10000000", message = "Maximum request amount is 10,000,000 TZS")
    @Schema(description = "Requested top-up amount (TZS)", example = "20000.00")
    private BigDecimal amount;

    @Schema(description = "Optional note or payment reference", example = "Bank transfer ref: 12345")
    private String note;
}
