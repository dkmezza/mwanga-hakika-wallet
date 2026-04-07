package com.mwanga.wallet.requisition;

import com.mwanga.wallet.common.ApiResponse;
import com.mwanga.wallet.common.PageResponse;
import com.mwanga.wallet.requisition.dto.RequisitionRequest;
import com.mwanga.wallet.requisition.dto.RequisitionResponse;
import com.mwanga.wallet.requisition.dto.RequisitionReviewRequest;
import com.mwanga.wallet.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requisitions")
@RequiredArgsConstructor
@Tag(name = "Top-Up Requisitions", description = "User top-up requests and admin approval workflow")
@SecurityRequirement(name = "bearerAuth")
public class RequisitionController {

    private final RequisitionService requisitionService;

    // ── User endpoints ─────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Submit a top-up request",
            description = "User requests to have their wallet credited. Awaits admin approval."
    )
    public ResponseEntity<ApiResponse<RequisitionResponse>> createRequisition(
            @Valid @RequestBody RequisitionRequest request,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Top-up request submitted successfully",
                        requisitionService.createRequisition(user, request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's own top-up requests")
    public ResponseEntity<ApiResponse<PageResponse<RequisitionResponse>>> getMyRequisitions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                requisitionService.getMyRequisitions(user, pageable)));
    }

    // ── Admin endpoints ────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all top-up requests (Admin only)",
               description = "Filter by status: PENDING, APPROVED, REJECTED. Leave blank for all.")
    public ResponseEntity<ApiResponse<PageResponse<RequisitionResponse>>> getAllRequisitions(
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                requisitionService.getAllRequisitions(status, pageable)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Approve a top-up request (Admin only)",
            description = "Credits the user's wallet and marks the requisition as APPROVED."
    )
    public ResponseEntity<ApiResponse<RequisitionResponse>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) RequisitionReviewRequest body,
            @AuthenticationPrincipal User admin) {

        String note = (body != null) ? body.getAdminNote() : null;
        return ResponseEntity.ok(ApiResponse.ok("Requisition approved",
                requisitionService.approveRequisition(id, admin, note)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Reject a top-up request (Admin only)",
            description = "Marks the requisition as REJECTED. Wallet balance is unchanged."
    )
    public ResponseEntity<ApiResponse<RequisitionResponse>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RequisitionReviewRequest body,
            @AuthenticationPrincipal User admin) {

        String note = (body != null) ? body.getAdminNote() : null;
        return ResponseEntity.ok(ApiResponse.ok("Requisition rejected",
                requisitionService.rejectRequisition(id, admin, note)));
    }
}
