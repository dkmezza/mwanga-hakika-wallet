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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Alice Mwangi");
        registerRequest.setEmail("alice@test.com");
        registerRequest.setPassword("Password@123");
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: happy path creates user (USER role) and a zero-balance TZS wallet")
    void register_newEmail_createsUserAndWallet() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$hashed");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder()
                    .id(UUID.randomUUID())
                    .email(u.getEmail())
                    .fullName(u.getFullName())
                    .password(u.getPassword())
                    .role(u.getRole())
                    .active(true)
                    .build();
        });
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
        assertThat(response.getFullName()).isEqualTo("Alice Mwangi");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("alice@test.com") &&
                u.getFullName().equals("Alice Mwangi") &&
                u.getRole() == Role.USER &&
                u.getPassword().equals("$2a$hashed")
        ));
        verify(walletRepository).save(argThat(w ->
                w.getBalance().compareTo(BigDecimal.ZERO) == 0 &&
                "TZS".equals(w.getCurrency())
        ));
    }

    @Test
    @DisplayName("register: email is normalised to lowercase before persistence")
    void register_emailNormalisedToLowercase() {
        registerRequest.setEmail("ALICE@TEST.COM");
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("tok");
        when(jwtService.generateRefreshToken(any())).thenReturn("ref");

        authService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getEmail().equals("alice@test.com")));
    }

    @Test
    @DisplayName("register: duplicate email throws DuplicateResourceException — no DB write")
    void register_existingEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials return access and refresh tokens")
    void login_validCredentials_returnsTokens() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@test.com")
                .fullName("Alice Mwangi")
                .password("$2a$hashed")
                .role(Role.USER)
                .active(true)
                .build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("Password@123");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
        // AuthenticationManager must be called so Spring Security validates the password
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: invalid password delegates BadCredentialsException to caller")
    void login_badPassword_propagatesBadCredentialsException() {
        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }
}
