package com.mwanga.wallet.transaction;

import com.mwanga.wallet.common.exception.ForbiddenException;
import com.mwanga.wallet.common.exception.InsufficientFundsException;
import com.mwanga.wallet.common.exception.TransactionLimitException;
import com.mwanga.wallet.config.WalletProperties;
import com.mwanga.wallet.transaction.dto.TransactionResponse;
import com.mwanga.wallet.transaction.dto.TransferRequest;
import com.mwanga.wallet.user.Role;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletRepository;
import com.mwanga.wallet.wallet.WalletService;
import com.mwanga.wallet.wallet.dto.AdminTopUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService unit tests")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletService walletService;
    @Mock private WalletProperties walletProperties;

    @InjectMocks
    private TransactionService transactionService;

    // UUIDs chosen so SENDER_ID < RECEIVER_ID for deterministic lock ordering
    private static final UUID SENDER_WALLET_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_WALLET_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private User sender;
    private User admin;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        // Wire transfer limits
        WalletProperties.Transfer transferProps = new WalletProperties.Transfer();
        transferProps.setMinAmount(new BigDecimal("100"));
        transferProps.setMaxAmount(new BigDecimal("5000000"));
        lenient().when(walletProperties.getTransfer()).thenReturn(transferProps);

        // Wire top-up limits
        WalletProperties.Topup topupProps = new WalletProperties.Topup();
        topupProps.setMinAmount(new BigDecimal("100"));
        topupProps.setMaxAmount(new BigDecimal("10000000"));
        lenient().when(walletProperties.getTopup()).thenReturn(topupProps);

        sender = User.builder().id(UUID.randomUUID()).email("sender@test.com")
                .role(Role.USER).active(true).build();
        admin  = User.builder().id(UUID.randomUUID()).email("admin@test.com")
                .role(Role.ADMIN).active(true).build();

        senderWallet   = buildWallet(SENDER_WALLET_ID,   sender,   "10000");
        receiverWallet = buildWallet(RECEIVER_WALLET_ID, null,     "5000");
    }

    // ── transfer: happy path ───────────────────────────────────────────────────

    @Test
    @DisplayName("transfer: deducts sender balance and credits receiver balance atomically")
    void transfer_success_updatesBalances() {
        TransferRequest request = transferRequest(RECEIVER_WALLET_ID, "1000", "key-1");
        stubNewReference("key-1");
        stubWalletLocks();
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenReturn(buildTx(TransactionType.TRANSFER));

        TransactionResponse result = transactionService.transfer(sender, request);

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        // Sender: 10000 − 1000 = 9000
        assertThat(senderWallet.getBalance()).isEqualByComparingTo("9000");
        // Receiver: 5000 + 1000 = 6000
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("6000");
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    // ── transfer: guard rails ──────────────────────────────────────────────────

    @Test
    @DisplayName("transfer: insufficient balance → InsufficientFundsException, no DB writes")
    void transfer_insufficientFunds_throwsAndNoDbWrite() {
        TransferRequest request = transferRequest(RECEIVER_WALLET_ID, "99999", "key-2");
        stubNewReference("key-2");
        stubWalletLocks();

        assertThatThrownBy(() -> transactionService.transfer(sender, request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("transfer: amount below 100 TZS minimum → TransactionLimitException")
    void transfer_belowMinimum_throwsTransactionLimitException() {
        TransferRequest request = transferRequest(RECEIVER_WALLET_ID, "50", "key-3");
        stubNewReference("key-3");
        when(walletService.requireActiveWalletByUserId(sender.getId())).thenReturn(senderWallet);
        when(walletService.requireActiveWallet(RECEIVER_WALLET_ID)).thenReturn(receiverWallet);

        assertThatThrownBy(() -> transactionService.transfer(sender, request))
                .isInstanceOf(TransactionLimitException.class)
                .hasMessageContaining("100");
    }

    @Test
    @DisplayName("transfer: amount above 5,000,000 TZS maximum → TransactionLimitException")
    void transfer_aboveMaximum_throwsTransactionLimitException() {
        TransferRequest request = transferRequest(RECEIVER_WALLET_ID, "9999999", "key-4");
        stubNewReference("key-4");
        when(walletService.requireActiveWalletByUserId(sender.getId())).thenReturn(senderWallet);
        when(walletService.requireActiveWallet(RECEIVER_WALLET_ID)).thenReturn(receiverWallet);

        assertThatThrownBy(() -> transactionService.transfer(sender, request))
                .isInstanceOf(TransactionLimitException.class)
                .hasMessageContaining("5000000");
    }

    @Test
    @DisplayName("transfer: sender → own wallet → ForbiddenException")
    void transfer_toOwnWallet_throwsForbiddenException() {
        // Sender's wallet IS the receiver wallet
        TransferRequest request = transferRequest(SENDER_WALLET_ID, "1000", "key-5");
        stubNewReference("key-5");
        when(walletService.requireActiveWalletByUserId(sender.getId())).thenReturn(senderWallet);
        when(walletService.requireActiveWallet(SENDER_WALLET_ID)).thenReturn(senderWallet);

        assertThatThrownBy(() -> transactionService.transfer(sender, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own wallet");
    }

    // ── transfer: idempotency ──────────────────────────────────────────────────

    @Test
    @DisplayName("transfer: duplicate idempotency key returns existing tx, no second DB write")
    void transfer_duplicateKey_returnsExistingTransaction() {
        Transaction existing = buildTx(TransactionType.TRANSFER);
        when(transactionRepository.findByReference("dup-key")).thenReturn(Optional.of(existing));

        TransferRequest request = transferRequest(RECEIVER_WALLET_ID, "1000", "dup-key");
        TransactionResponse result = transactionService.transfer(sender, request);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verifyNoInteractions(walletService);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // ── adminTopUp ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("adminTopUp: happy path credits receiver wallet, creates TOP_UP transaction")
    void adminTopUp_success_creditsWallet() {
        AdminTopUpRequest request = new AdminTopUpRequest();
        request.setAmount(new BigDecimal("5000"));
        request.setDescription("Admin credit");
        request.setIdempotencyKey("topup-key-1");

        when(transactionRepository.findByReference("topup-key-1")).thenReturn(Optional.empty());
        when(walletService.requireActiveWalletByUserId(any())).thenReturn(receiverWallet);
        when(walletRepository.findByIdWithLock(RECEIVER_WALLET_ID)).thenReturn(Optional.of(receiverWallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenReturn(buildTx(TransactionType.TOP_UP));

        transactionService.adminTopUp(UUID.randomUUID(), request, admin);

        // 5000 (initial) + 5000 (top-up) = 10000
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo("10000");
        verify(walletRepository).save(receiverWallet);
    }

    @Test
    @DisplayName("adminTopUp: duplicate key returns existing tx without double-crediting")
    void adminTopUp_duplicateKey_returnsExistingTransaction() {
        Transaction existing = buildTx(TransactionType.TOP_UP);
        when(transactionRepository.findByReference("topup-dup")).thenReturn(Optional.of(existing));

        AdminTopUpRequest request = new AdminTopUpRequest();
        request.setAmount(new BigDecimal("5000"));
        request.setIdempotencyKey("topup-dup");

        TransactionResponse result = transactionService.adminTopUp(UUID.randomUUID(), request, admin);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verifyNoInteractions(walletService);
        verify(walletRepository, never()).save(any());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void stubNewReference(String key) {
        when(transactionRepository.findByReference(key)).thenReturn(Optional.empty());
    }

    private void stubWalletLocks() {
        when(walletService.requireActiveWalletByUserId(sender.getId())).thenReturn(senderWallet);
        when(walletService.requireActiveWallet(RECEIVER_WALLET_ID)).thenReturn(receiverWallet);
        // Both wallets must be findable by lock — order depends on UUID comparison
        when(walletRepository.findByIdWithLock(SENDER_WALLET_ID)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdWithLock(RECEIVER_WALLET_ID)).thenReturn(Optional.of(receiverWallet));
    }

    private TransferRequest transferRequest(UUID receiverId, String amount, String key) {
        TransferRequest req = new TransferRequest();
        req.setReceiverWalletId(receiverId);
        req.setAmount(new BigDecimal(amount));
        req.setIdempotencyKey(key);
        return req;
    }

    private Wallet buildWallet(UUID id, User user, String balance) {
        return Wallet.builder()
                .id(id)
                .user(user)
                .balance(new BigDecimal(balance))
                .currency("TZS")
                .active(true)
                .version(0L)
                .build();
    }

    private Transaction buildTx(TransactionType type) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .reference(UUID.randomUUID().toString())
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .senderWallet(type == TransactionType.TRANSFER ? senderWallet : null)
                .receiverWallet(receiverWallet)
                .amount(new BigDecimal("1000"))
                .fee(BigDecimal.ZERO)
                .initiatedBy(type == TransactionType.TRANSFER ? sender : admin)
                .createdAt(Instant.now())
                .build();
    }
}
