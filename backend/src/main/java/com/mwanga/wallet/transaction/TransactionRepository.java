package com.mwanga.wallet.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    /**
     * Returns all transactions where a wallet is either the sender or the receiver,
     * ordered newest-first. Handles null senderWallet (TOP_UP rows) safely.
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.senderWallet IS NOT NULL AND t.senderWallet.id = :walletId)
               OR t.receiverWallet.id = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    /** Admin view — all transactions newest-first. */
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
