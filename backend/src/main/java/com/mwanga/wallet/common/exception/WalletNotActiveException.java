package com.mwanga.wallet.common.exception;

public class WalletNotActiveException extends RuntimeException {

    public WalletNotActiveException(String message) {
        super(message);
    }
}
