package com.mwanga.wallet.auth;

import com.mwanga.wallet.auth.dto.AuthResponse;
import com.mwanga.wallet.auth.dto.LoginRequest;
import com.mwanga.wallet.auth.dto.RegisterRequest;
import com.mwanga.wallet.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a USER-role account and provisions a TZS wallet. Returns JWT tokens."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and obtain tokens",
            description = "Returns an access token (24 h) and a refresh token (7 days)."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Pass the refresh token in the X-Refresh-Token header. Returns a new token pair."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        return ResponseEntity.ok(
                ApiResponse.ok("Token refreshed", authService.refresh(refreshToken)));
    }
}
