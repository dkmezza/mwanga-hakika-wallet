package com.mwanga.wallet.auth.dto;

import com.mwanga.wallet.user.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Returned after a successful login or register. Contains both tokens so
 * the client can store the refresh token securely and use access token for API calls.
 */
@Getter
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;

    @Builder.Default
    private final String tokenType = "Bearer";

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final Role role;
}
