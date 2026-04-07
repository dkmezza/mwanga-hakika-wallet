package com.mwanga.wallet.transaction;

public enum TransactionStatus {
    /** Created but not yet settled (reserved for async flows). */
    PENDING,

    /** Funds successfully moved. */
    COMPLETED,

    /** Operation failed; balances were not changed. */
    FAILED
}
