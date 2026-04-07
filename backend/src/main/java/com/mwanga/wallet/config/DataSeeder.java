package com.mwanga.wallet.config;

import com.mwanga.wallet.user.Role;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.user.UserRepository;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Dev/staging seed data. Skipped on the test profile — integration tests
 * manage their own fixtures.
 *
 * Seeded accounts (idempotent, safe to re-run):
 *   admin@mwanga.co.tz  / Admin@1234
 *   alice@example.com   / User@1234  (50,000 TZS)
 *   bob@example.com     / User@1234  (25,000 TZS)
 *
 * Do NOT ship these credentials to prod.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedUser("Alice Mwangi",   "alice@example.com", new BigDecimal("50000.0000"));
        seedUser("Bob Kamau",      "bob@example.com",   new BigDecimal("25000.0000"));
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@mwanga.co.tz")) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .fullName("System Administrator")
                .email("admin@mwanga.co.tz")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(Role.ADMIN)
                .active(true)
                .build());

        walletRepository.save(Wallet.builder()
                .user(admin)
                .balance(BigDecimal.ZERO)
                .currency("TZS")
                .active(true)
                .build());

        log.info("Seeded ADMIN: admin@mwanga.co.tz");
    }

    private void seedUser(String fullName, String email, BigDecimal initialBalance) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = userRepository.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode("User@1234"))
                .role(Role.USER)
                .active(true)
                .build());

        walletRepository.save(Wallet.builder()
                .user(user)
                .balance(initialBalance)
                .currency("TZS")
                .active(true)
                .build());

        log.info("Seeded USER: {} (balance: {} TZS)", email, initialBalance);
    }
}
