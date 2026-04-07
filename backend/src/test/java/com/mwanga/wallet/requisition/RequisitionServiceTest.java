package com.mwanga.wallet.requisition;

import com.mwanga.wallet.common.exception.ForbiddenException;
import com.mwanga.wallet.requisition.dto.RequisitionRequest;
import com.mwanga.wallet.requisition.dto.RequisitionResponse;
import com.mwanga.wallet.transaction.TransactionService;
import com.mwanga.wallet.transaction.dto.TransactionResponse;
import com.mwanga.wallet.user.Role;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequisitionService unit tests")
class RequisitionServiceTest {

    @Mock private RequisitionRepository requisitionRepository;
    @Mock private WalletService walletService;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private RequisitionService requisitionService;

    private User user;
    private User admin;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("user@test.com")
                .role(Role.USER).active(true).build();
        admin = User.builder().id(UUID.randomUUID()).email("admin@test.com")
                .role(Role.ADMIN).active(true).build();
        wallet = Wallet.builder().id(UUID.randomUUID()).user(user)
                .balance(new BigDecimal("5000")).currency("TZS").active(true).build();
    }

    // ── createRequisition ──────────────────────────────────────────────────────

    @Test
    @DisplayName("createRequisition: saves PENDING requisition and returns DTO")
    void createRequisition_success_returnsPendingRequisition() {
        RequisitionRequest request = new RequisitionRequest();
        request.setAmount(new BigDecimal("10000"));
        request.setNote("Bank transfer ref: ABC123");

        when(walletService.requireActiveWalletByUserId(user.getId())).thenReturn(wallet);

        TopUpRequisition saved = TopUpRequisition.builder()
                .id(UUID.randomUUID())
                .user(user)
                .wallet(wallet)
                .requestedAmount(new BigDecimal("10000"))
                .status(RequisitionStatus.PENDING)
                .note("Bank transfer ref: ABC123")
                .build();
        when(requisitionRepository.save(any())).thenReturn(saved);

        RequisitionResponse response = requisitionService.createRequisition(user, request);

        assertThat(response.getStatus()).isEqualTo(RequisitionStatus.PENDING);
        assertThat(response.getRequestedAmount()).isEqualByComparingTo("10000");
        assertThat(response.getNote()).isEqualTo("Bank transfer ref: ABC123");
        verify(requisitionRepository).save(any(TopUpRequisition.class));
        verifyNoInteractions(transactionService);
    }

    // ── approveRequisition ─────────────────────────────────────────────────────

    @Test
    @DisplayName("approveRequisition: calls executeTopUp, sets APPROVED status and admin metadata")
    void approveRequisition_success_creditsWalletAndUpdatesStatus() {
        TopUpRequisition pending = buildRequisition(RequisitionStatus.PENDING);

        when(requisitionRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(transactionService.executeTopUp(any(), any(), any(), any(), any()))
                .thenReturn(mock(TransactionResponse.class));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequisitionResponse response = requisitionService.approveRequisition(
                pending.getId(), admin, "Payment confirmed");

        assertThat(response.getStatus()).isEqualTo(RequisitionStatus.APPROVED);
        assertThat(response.getAdminNote()).isEqualTo("Payment confirmed");
        assertThat(response.getReviewedById()).isEqualTo(admin.getId());
        assertThat(response.getReviewedAt()).isNotNull();

        // Verify the wallet credit delegate was called with correct amount and wallet
        verify(transactionService).executeTopUp(
                eq(wallet),
                eq(new BigDecimal("10000")),
                any(String.class),
                any(String.class),
                eq(admin)
        );
    }

    @Test
    @DisplayName("approveRequisition: already-APPROVED requisition throws ForbiddenException")
    void approveRequisition_alreadyApproved_throwsForbiddenException() {
        TopUpRequisition alreadyApproved = buildRequisition(RequisitionStatus.APPROVED);
        when(requisitionRepository.findById(alreadyApproved.getId()))
                .thenReturn(Optional.of(alreadyApproved));

        assertThatThrownBy(() ->
                requisitionService.approveRequisition(alreadyApproved.getId(), admin, "note"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("approved");

        verifyNoInteractions(transactionService);
    }

    @Test
    @DisplayName("approveRequisition: REJECTED requisition also throws ForbiddenException")
    void approveRequisition_alreadyRejected_throwsForbiddenException() {
        TopUpRequisition rejected = buildRequisition(RequisitionStatus.REJECTED);
        when(requisitionRepository.findById(rejected.getId()))
                .thenReturn(Optional.of(rejected));

        assertThatThrownBy(() ->
                requisitionService.approveRequisition(rejected.getId(), admin, "note"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── rejectRequisition ──────────────────────────────────────────────────────

    @Test
    @DisplayName("rejectRequisition: sets REJECTED status, no wallet credit occurs")
    void rejectRequisition_success_updatesStatusWithoutCreditingWallet() {
        TopUpRequisition pending = buildRequisition(RequisitionStatus.PENDING);

        when(requisitionRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(requisitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequisitionResponse response = requisitionService.rejectRequisition(
                pending.getId(), admin, "Insufficient documentation provided");

        assertThat(response.getStatus()).isEqualTo(RequisitionStatus.REJECTED);
        assertThat(response.getAdminNote()).isEqualTo("Insufficient documentation provided");
        assertThat(response.getReviewedById()).isEqualTo(admin.getId());

        // Critical: wallet must NOT be credited on rejection
        verifyNoInteractions(transactionService);
        verifyNoInteractions(walletService);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private TopUpRequisition buildRequisition(RequisitionStatus status) {
        return TopUpRequisition.builder()
                .id(UUID.randomUUID())
                .user(user)
                .wallet(wallet)
                .requestedAmount(new BigDecimal("10000"))
                .status(status)
                .build();
    }
}
