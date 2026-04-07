package com.mwanga.wallet.wallet;

import com.mwanga.wallet.common.ApiResponse;
import com.mwanga.wallet.common.PageResponse;
import com.mwanga.wallet.transaction.TransactionService;
import com.mwanga.wallet.transaction.dto.TransactionResponse;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.dto.AdminTopUpRequest;
import com.mwanga.wallet.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Balance enquiry, admin top-up, and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's wallet and balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal User principal) {

        return ResponseEntity.ok(ApiResponse.ok(walletService.getMyWallet(principal)));
    }

    @GetMapping("/me/transactions")
    @Operation(summary = "Get the authenticated user's transaction history (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.getMyTransactions(principal, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get any wallet by ID (Admin only)")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(walletService.getWalletById(id)));
    }

    @PostMapping("/{userId}/topup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: credit a user's wallet directly",
            description = "Creates a TOP_UP transaction and increases the target wallet balance atomically."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> adminTopUp(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminTopUpRequest request,
            @AuthenticationPrincipal User admin) {

        TransactionResponse tx = transactionService.adminTopUp(userId, request, admin);
        return ResponseEntity.ok(ApiResponse.ok("Wallet topped up successfully", tx));
    }
}
