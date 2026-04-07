package com.mwanga.wallet.user.dto;

import com.mwanga.wallet.user.Role;
import com.mwanga.wallet.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe public view of a user — never expose password or internal fields.
 */
@Getter
@Builder
public class UserResponse {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
