package com.mwanga.wallet.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, String field, Object value) {
        return new ResourceNotFoundException(
                String.format("%s not found with %s: %s", resource, field, value)
        );
    }
}
