package com.mwanga.wallet.auth;

import com.mwanga.wallet.auth.dto.AuthResponse;
import com.mwanga.wallet.auth.dto.LoginRequest;
import com.mwanga.wallet.auth.dto.RegisterRequest;
import com.mwanga.wallet.common.exception.DuplicateResourceException;
import com.mwanga.wallet.security.JwtService;
import com.mwanga.wallet.user.Role;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.user.UserRepository;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Creates the user and provisions their wallet in a single transaction.
     * Email is lower-cased here — letting a DB unique constraint surface it would give a 500, not a 409.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .active(true)
                .build();

        user = userRepository.save(user);

        // one wallet per user, zero balance, TZS only for now
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency("TZS")
                .active(true)
                .build();
        walletRepository.save(wallet);

        log.info("New user registered: id={}, email={}", user.getId(), email);

        return buildAuthResponse(user);
    }

    /**
     * Validates credentials via AuthenticationManager then issues a fresh token pair.
     */
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // throws BadCredentialsException on bad creds — GlobalExceptionHandler maps it to 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email).orElseThrow();

        log.info("User logged in: id={}, email={}", user.getId(), email);

        return buildAuthResponse(user);
    }

    /**
     * Verifies the refresh token (signature + expiry) then issues a new access + refresh pair.
     */
    public AuthResponse refresh(String refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token is expired or invalid");
        }

        log.info("Token refreshed for user: {}", userEmail);

        return buildAuthResponse(user);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
