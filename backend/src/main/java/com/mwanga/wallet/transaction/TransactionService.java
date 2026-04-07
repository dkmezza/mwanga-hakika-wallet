package com.mwanga.wallet.transaction;

import com.mwanga.wallet.common.PageResponse;
import com.mwanga.wallet.common.exception.ForbiddenException;
import com.mwanga.wallet.common.exception.InsufficientFundsException;
import com.mwanga.wallet.common.exception.ResourceNotFoundException;
import com.mwanga.wallet.common.exception.TransactionLimitException;
import com.mwanga.wallet.config.WalletProperties;
import com.mwanga.wallet.transaction.dto.TransactionResponse;
import com.mwanga.wallet.transaction.dto.TransferRequest;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletRepository;
import com.mwanga.wallet.wallet.WalletService;
import com.mwanga.wallet.wallet.dto.AdminTopUpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * All money movement lives here — P2P transfers and admin top-ups.
 *
 * Concurrency: wallets are locked with SELECT FOR UPDATE in ascending UUID order.
 * That ordering is what prevents the AB-BA deadlock when two concurrent transfers
 * hit the same wallet pair going in opposite directions.
 *
 * Idempotency: callers pass an idempotencyKey (or we generate one). Same key =
 * same response, no double-debit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final WalletProperties walletProperties;

    // ── Public: peer-to-peer transfer ─────────────────────────────────────────

    @Transactional
    public TransactionResponse transfer(User sender, TransferRequest request) {
        String reference = resolveReference(request.getIdempotencyKey());

        // short-circuit if this reference already landed
        return transactionRepository.findByReference(reference)
                .map(TransactionResponse::from)
                .orElseGet(() -> executeTransfer(sender, request, reference));
    }

    // ── Public: admin top-up ───────────────────────────────────────────────────

    @Transactional
    public TransactionResponse adminTopUp(UUID targetUserId, AdminTopUpRequest request, User admin) {
        String reference = resolveReference(request.getIdempotencyKey());

        return transactionRepository.findByReference(reference)
                .map(TransactionResponse::from)
                .orElseGet(() -> executeTopUp(
                        walletService.requireActiveWalletByUserId(targetUserId),
                        request.getAmount(),
                        reference,
                        request.getDescription(),
                        admin
                ));
    }

    // ── Shared: also called by RequisitionService on approval ─────────────────

    /**
     * Credits a wallet and persists a TOP_UP record.
     * Public because RequisitionService lives in a sibling package.
     * Callers are responsible for passing a valid, active wallet.
     */
    @Transactional
    public TransactionResponse executeTopUp(Wallet receiverWallet,
                                     BigDecimal amount,
                                     String reference,
                                     String description,
                                     User initiatedBy) {
        validateTopUpAmount(amount);

        // grab the write lock before touching the balance
        Wallet locked = walletRepository.findByIdWithLock(receiverWallet.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet", "id", receiverWallet.getId()));

        locked.setBalance(locked.getBalance().add(amount));
        walletRepository.save(locked);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .reference(reference)
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.COMPLETED)
                .senderWallet(null)
                .receiverWallet(locked)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .description(description)
                .initiatedBy(initiatedBy)
                .build());

        log.info("TOP_UP ref={} amount={} wallet={} by={}",
                reference, amount, locked.getId(), initiatedBy.getId());

        return TransactionResponse.from(tx);
    }

    // ── Public: queries ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getMyTransactions(User user, Pageable pageable) {
        Wallet wallet = walletService.requireActiveWalletByUserId(user.getId());
        return PageResponse.from(
                transactionRepository.findByWalletId(wallet.getId(), pageable)
                        .map(TransactionResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id, User viewer) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", "id", id));

        // non-admins can only view transactions they participated in
        boolean isAdmin = viewer.getRole().name().equals("ADMIN");
        if (!isAdmin) {
            UUID viewerWalletId = walletService.requireActiveWalletByUserId(viewer.getId()).getId();
            boolean isParticipant =
                    (tx.getSenderWallet() != null && tx.getSenderWallet().getId().equals(viewerWalletId))
                    || tx.getReceiverWallet().getId().equals(viewerWalletId);
            if (!isParticipant) {
                throw new ForbiddenException("You do not have access to this transaction");
            }
        }

        return TransactionResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getAllTransactions(Pageable pageable) {
        return PageResponse.from(
                transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(TransactionResponse::from)
        );
    }

    // ── Private: core transfer logic ───────────────────────────────────────────

    private TransactionResponse executeTransfer(User sender, TransferRequest request, String reference) {
        validateTransferAmount(request.getAmount());

        Wallet senderWallet = walletService.requireActiveWalletByUserId(sender.getId());
        Wallet receiverWallet = walletService.requireActiveWallet(request.getReceiverWalletId());

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new ForbiddenException("Cannot transfer funds to your own wallet");
        }

        // deterministic lock order prevents AB-BA deadlock on concurrent reverse transfers
        Wallet[] locked = lockWalletsInOrder(senderWallet.getId(), receiverWallet.getId());
        Wallet lockedSender   = locked[0].getId().equals(senderWallet.getId())   ? locked[0] : locked[1];
        Wallet lockedReceiver = locked[0].getId().equals(receiverWallet.getId()) ? locked[0] : locked[1];

        if (lockedSender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + lockedSender.getBalance() + " TZS");
        }

        lockedSender.setBalance(lockedSender.getBalance().subtract(request.getAmount()));
        lockedReceiver.setBalance(lockedReceiver.getBalance().add(request.getAmount()));

        walletRepository.save(lockedSender);
        walletRepository.save(lockedReceiver);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .reference(reference)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .senderWallet(lockedSender)
                .receiverWallet(lockedReceiver)
                .amount(request.getAmount())
                .fee(BigDecimal.ZERO)
                .description(request.getDescription())
                .initiatedBy(sender)
                .build());

        log.info("TRANSFER ref={} amount={} from={} to={} by={}",
                reference, request.getAmount(),
                lockedSender.getId(), lockedReceiver.getId(), sender.getId());

        return TransactionResponse.from(tx);
    }

    /**
     * Locks both wallets in UUID-ascending order, returns [smaller, larger].
     * Caller must map back to sender/receiver by ID — array position doesn't encode that.
     */
    private Wallet[] lockWalletsInOrder(UUID id1, UUID id2) {
        boolean firstIsSmaller = id1.compareTo(id2) < 0;
        UUID smallerId = firstIsSmaller ? id1 : id2;
        UUID largerId  = firstIsSmaller ? id2 : id1;

        Wallet small = walletRepository.findByIdWithLock(smallerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet", "id", smallerId));
        Wallet large = walletRepository.findByIdWithLock(largerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Wallet", "id", largerId));

        return new Wallet[]{small, large};
    }

    // ── Validation ─────────────────────────────────────────────────────────────

    private void validateTransferAmount(BigDecimal amount) {
        BigDecimal min = walletProperties.getTransfer().getMinAmount();
        BigDecimal max = walletProperties.getTransfer().getMaxAmount();
        if (amount.compareTo(min) < 0) {
            throw new TransactionLimitException(
                    "Transfer amount must be at least " + min + " TZS");
        }
        if (amount.compareTo(max) > 0) {
            throw new TransactionLimitException(
                    "Transfer amount must not exceed " + max + " TZS");
        }
    }

    private void validateTopUpAmount(BigDecimal amount) {
        BigDecimal min = walletProperties.getTopup().getMinAmount();
        BigDecimal max = walletProperties.getTopup().getMaxAmount();
        if (amount.compareTo(min) < 0) {
            throw new TransactionLimitException(
                    "Top-up amount must be at least " + min + " TZS");
        }
        if (amount.compareTo(max) > 0) {
            throw new TransactionLimitException(
                    "Top-up amount must not exceed " + max + " TZS");
        }
    }

    private String resolveReference(String clientKey) {
        return (clientKey != null && !clientKey.isBlank())
                ? clientKey
                : UUID.randomUUID().toString();
    }
}
