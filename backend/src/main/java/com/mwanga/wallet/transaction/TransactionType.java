package com.mwanga.wallet.transaction;

public enum TransactionType {
    /** Admin credits a user's wallet directly. */
    TOP_UP,

    /** User-to-user fund movement. */
    TRANSFER
}
