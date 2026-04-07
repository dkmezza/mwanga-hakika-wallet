package com.mwanga.wallet.requisition;

import com.mwanga.wallet.common.PageResponse;
import com.mwanga.wallet.common.exception.ForbiddenException;
import com.mwanga.wallet.common.exception.ResourceNotFoundException;
import com.mwanga.wallet.requisition.dto.RequisitionRequest;
import com.mwanga.wallet.requisition.dto.RequisitionResponse;
import com.mwanga.wallet.transaction.TransactionService;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;

    // ── User operations ────────────────────────────────────────────────────────

    @Transactional
    public RequisitionResponse createRequisition(User user, RequisitionRequest request) {
        Wallet wallet = walletService.requireActiveWalletByUserId(user.getId());

        TopUpRequisition requisition = requisitionRepository.save(
                TopUpRequisition.builder()
                        .user(user)
                        .wallet(wallet)
                        .requestedAmount(request.getAmount())
                        .status(RequisitionStatus.PENDING)
                        .note(request.getNote())
                        .build()
        );

        log.info("Top-up requisition created: id={} user={} amount={}",
                requisition.getId(), user.getId(), request.getAmount());

        return RequisitionResponse.from(requisition);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequisitionResponse> getMyRequisitions(User user, Pageable pageable) {
        return PageResponse.from(
                requisitionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                        .map(RequisitionResponse::from)
        );
    }

    // ── Admin operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<RequisitionResponse> getAllRequisitions(RequisitionStatus status,
                                                                Pageable pageable) {
        var page = (status != null)
                ? requisitionRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : requisitionRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponse.from(page.map(RequisitionResponse::from));
    }

    @Transactional
    public RequisitionResponse approveRequisition(UUID id, User admin, String adminNote) {
        TopUpRequisition requisition = findPendingOrThrow(id);

        // delegate to TransactionService — it owns all balance mutations
        transactionService.executeTopUp(
                requisition.getWallet(),
                requisition.getRequestedAmount(),
                UUID.randomUUID().toString(),   // fresh ref; no client idempotency key on approval
                "Top-up requisition approved: " + id,
                admin
        );

        requisition.setStatus(RequisitionStatus.APPROVED);
        requisition.setAdminNote(adminNote);
        requisition.setReviewedBy(admin);
        requisition.setReviewedAt(Instant.now());

        TopUpRequisition saved = requisitionRepository.save(requisition);

        log.info("Requisition {} APPROVED by admin={} amount={}",
                id, admin.getId(), requisition.getRequestedAmount());

        return RequisitionResponse.from(saved);
    }

    @Transactional
    public RequisitionResponse rejectRequisition(UUID id, User admin, String adminNote) {
        TopUpRequisition requisition = findPendingOrThrow(id);

        requisition.setStatus(RequisitionStatus.REJECTED);
        requisition.setAdminNote(adminNote);
        requisition.setReviewedBy(admin);
        requisition.setReviewedAt(Instant.now());

        TopUpRequisition saved = requisitionRepository.save(requisition);

        log.info("Requisition {} REJECTED by admin={}", id, admin.getId());

        return RequisitionResponse.from(saved);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private TopUpRequisition findPendingOrThrow(UUID id) {
        TopUpRequisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Requisition", "id", id));

        if (req.getStatus() != RequisitionStatus.PENDING) {
            throw new ForbiddenException(
                    "Requisition " + id + " has already been " + req.getStatus().name().toLowerCase());
        }
        return req;
    }
}
