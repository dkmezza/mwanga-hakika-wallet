package com.mwanga.wallet.transaction;

import com.mwanga.wallet.common.ApiResponse;
import com.mwanga.wallet.common.PageResponse;
import com.mwanga.wallet.transaction.dto.TransactionResponse;
import com.mwanga.wallet.transaction.dto.TransferRequest;
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
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Fund transfers and transaction history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(
            summary = "Transfer funds to another wallet",
            description = """
                    Atomically deducts from the sender and credits the receiver.
                    Limits: min 100 TZS, max 5,000,000 TZS per transaction.
                    Supply an idempotencyKey to make retries safe.
                    """
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User sender) {

        TransactionResponse tx = transactionService.transfer(sender, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transfer completed successfully", tx));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a transaction by ID",
            description = "Users may only retrieve transactions they participated in. Admins see all."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User viewer) {

        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.getTransactionById(id, viewer)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all transactions (Admin only, paginated)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.getAllTransactions(pageable)));
    }
}
