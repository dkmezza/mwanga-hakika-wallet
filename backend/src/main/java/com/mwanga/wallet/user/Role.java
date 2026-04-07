package com.mwanga.wallet.user;

/**
 * Platform roles.
 * ADMIN — full system access (top-up, approve/reject requisitions, manage users).
 * USER  — standard wallet holder (transfer, request top-up, view own data).
 */
public enum Role {
    ADMIN,
    USER
}
