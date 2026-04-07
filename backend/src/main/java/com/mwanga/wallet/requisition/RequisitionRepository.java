package com.mwanga.wallet.requisition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RequisitionRepository extends JpaRepository<TopUpRequisition, UUID> {

    /** User's own requisitions, newest-first. */
    Page<TopUpRequisition> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Admin: all requisitions filtered by status, newest-first. */
    Page<TopUpRequisition> findByStatusOrderByCreatedAtDesc(RequisitionStatus status, Pageable pageable);

    /** Admin: all requisitions regardless of status, newest-first. */
    Page<TopUpRequisition> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
