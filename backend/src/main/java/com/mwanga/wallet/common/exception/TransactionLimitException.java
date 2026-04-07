package com.mwanga.wallet.common.exception;

public class TransactionLimitException extends RuntimeException {

    public TransactionLimitException(String message) {
        super(message);
    }
}
