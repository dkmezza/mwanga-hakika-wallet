package com.mwanga.wallet.requisition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Admin review decision (approve or reject)")
public class RequisitionReviewRequest {

    @Schema(description = "Admin note — required when rejecting, optional when approving",
            example = "Payment confirmed via bank reference")
    private String adminNote;
}
