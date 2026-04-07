package com.mwanga.wallet.wallet;

import com.mwanga.wallet.common.exception.ResourceNotFoundException;
import com.mwanga.wallet.common.exception.WalletNotActiveException;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(User principal) {
        Wallet wallet = findByUserOrThrow(principal.getId());
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(UUID walletId) {
        return WalletResponse.from(findOrThrow(walletId));
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        return WalletResponse.from(findByUserOrThrow(userId));
    }

    // Called by TransactionService and RequisitionService — not part of the REST surface

    /**
     * Loads a wallet and guards against inactive ones. Domain exceptions are thrown
     * so GlobalExceptionHandler can map them to the right HTTP status codes.
     */
    @Transactional(readOnly = true)
    public Wallet requireActiveWallet(UUID walletId) {
        Wallet wallet = findOrThrow(walletId);
        if (!wallet.isActive()) {
            throw new WalletNotActiveException("Wallet " + walletId + " is not active");
        }
        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet requireActiveWalletByUserId(UUID userId) {
        Wallet wallet = findByUserOrThrow(userId);
        if (!wallet.isActive()) {
            throw new WalletNotActiveException("Wallet for user " + userId + " is not active");
        }
        return wallet;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Wallet findOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet", "id", walletId));
    }

    private Wallet findByUserOrThrow(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet", "userId", userId));
    }
}
