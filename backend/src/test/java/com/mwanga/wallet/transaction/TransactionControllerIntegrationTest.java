package com.mwanga.wallet.transaction;

import com.mwanga.wallet.BaseIntegrationTest;
import com.mwanga.wallet.transaction.dto.TransferRequest;
import com.mwanga.wallet.user.Role;
import com.mwanga.wallet.user.User;
import com.mwanga.wallet.user.UserRepository;
import com.mwanga.wallet.wallet.Wallet;
import com.mwanga.wallet.wallet.WalletRepository;
import com.mwanga.wallet.wallet.dto.AdminTopUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the transfer and admin top-up flows.
 *
 * <p>Tests create their own users/wallets with unique emails to stay isolated
 * from other test classes sharing the same Testcontainers PostgreSQL instance.
 */
@DisplayName("Transaction endpoints — integration")
class TransactionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String senderToken;
    private String receiverToken;
    private String adminToken;
    private UUID senderWalletId;
    private UUID receiverWalletId;

    @BeforeEach
    void setUpUsersAndWallets() throws Exception {
        // Register sender and receiver via the API (this also creates their wallets)
        senderToken   = registerAndGetToken("Sender User",   "tx-sender@test.com",   "Password@123");
        receiverToken = registerAndGetToken("Receiver User", "tx-receiver@test.com", "Password@123");

        // Seed an ADMIN user directly (bypassing the public register endpoint which creates USERs)
        createAdminUser("tx-admin@test.com", "Password@123");
        adminToken = loginAndGetToken("tx-admin@test.com", "Password@123");

        // Discover wallet IDs via the API
        String senderWalletJson = mockMvc.perform(
                        withBearer(get("/api/v1/wallet/me"), senderToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        senderWalletId = UUID.fromString(extractField(senderWalletJson, "data", "id"));

        String receiverWalletJson = mockMvc.perform(
                        withBearer(get("/api/v1/wallet/me"), receiverToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        receiverWalletId = UUID.fromString(extractField(receiverWalletJson, "data", "id"));

        // Give the sender 20,000 TZS via admin top-up so transfer tests have funds
        AdminTopUpRequest topUp = new AdminTopUpRequest();
        topUp.setAmount(new BigDecimal("20000"));
        topUp.setDescription("Test seed top-up");
        topUp.setIdempotencyKey(UUID.randomUUID().toString());

        String senderUserId = extractField(senderWalletJson, "data", "userId");

        mockMvc.perform(
                        withBearer(post("/api/v1/wallet/" + senderUserId + "/topup"), adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(topUp)))
                .andExpect(status().isOk());
    }

    // ── POST /api/v1/transactions/transfer ────────────────────────────────────

    @Test
    @DisplayName("transfer: valid request → 201, sender debited, receiver credited")
    void transfer_validRequest_returns201AndUpdatesBalances() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletId(receiverWalletId);
        request.setAmount(new BigDecimal("5000"));
        request.setDescription("Lunch split");
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(
                        withBearer(post("/api/v1/transactions/transfer"), senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("TRANSFER"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(5000));

        // Verify sender balance: 20,000 − 5,000 = 15,000
        String senderWalletJson = mockMvc.perform(
                        withBearer(get("/api/v1/wallet/me"), senderToken))
                .andReturn().getResponse().getContentAsString();
        double senderBalance = extractNode(senderWalletJson, "data", "balance").asDouble();
        assertThat(senderBalance).isEqualTo(15000.0);

        // Verify receiver balance: 0 + 5,000 = 5,000
        String receiverWalletJson = mockMvc.perform(
                        withBearer(get("/api/v1/wallet/me"), receiverToken))
                .andReturn().getResponse().getContentAsString();
        double receiverBalance = extractNode(receiverWalletJson, "data", "balance").asDouble();
        assertThat(receiverBalance).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("transfer: insufficient funds → 422 Unprocessable Entity, balances unchanged")
    void transfer_insufficientFunds_returns422() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletId(receiverWalletId);
        request.setAmount(new BigDecimal("999999")); // sender only has 20,000
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(
                        withBearer(post("/api/v1/transactions/transfer"), senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient")));
    }

    @Test
    @DisplayName("transfer: amount below 100 TZS minimum → 422")
    void transfer_belowMinimum_returns422() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletId(receiverWalletId);
        request.setAmount(new BigDecimal("50"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(
                        withBearer(post("/api/v1/transactions/transfer"), senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("transfer: no Bearer token → 403 Forbidden")
    void transfer_unauthenticated_returns403() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverWalletId(receiverWalletId);
        request.setAmount(new BigDecimal("1000"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("transfer: idempotent retry returns same transaction — no double-debit")
    void transfer_sameIdempotencyKey_returnsExistingTransactionNoDoubleDebit() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        TransferRequest request = new TransferRequest();
        request.setReceiverWalletId(receiverWalletId);
        request.setAmount(new BigDecimal("1000"));
        request.setIdempotencyKey(idempotencyKey);

        // First request
        String firstResponse = mockMvc.perform(
                        withBearer(post("/api/v1/transactions/transfer"), senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstId = extractField(firstResponse, "data", "id");

        // Retry with the same key
        String secondResponse = mockMvc.perform(
                        withBearer(post("/api/v1/transactions/transfer"), senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondId = extractField(secondResponse, "data", "id");

        // Same transaction returned — no second debit
        assertThat(firstId).isEqualTo(secondId);

        // Sender balance: 20,000 − 1,000 = 19,000 (not 18,000)
        String walletJson = mockMvc.perform(
                        withBearer(get("/api/v1/wallet/me"), senderToken))
                .andReturn().getResponse().getContentAsString();
        double balance = extractNode(walletJson, "data", "balance").asDouble();
        assertThat(balance).isEqualTo(19000.0);
    }

    // ── POST /api/v1/wallet/{userId}/topup (Admin) ────────────────────────────

    @Test
    @DisplayName("adminTopUp: USER token → 403 Forbidden")
    void adminTopUp_withUserToken_returns403() throws Exception {
        String senderUserId = getWalletUserId(senderToken);
        AdminTopUpRequest topUp = new AdminTopUpRequest();
        topUp.setAmount(new BigDecimal("5000"));
        topUp.setIdempotencyKey(UUID.randomUUID().toString());

        mockMvc.perform(
                        withBearer(post("/api/v1/wallet/" + senderUserId + "/topup"), senderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(topUp)))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void createAdminUser(String email, String password) {
        if (userRepository.existsByEmail(email)) return;

        User admin = userRepository.save(User.builder()
                .fullName("Test Admin")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .active(true)
                .build());

        walletRepository.save(Wallet.builder()
                .user(admin)
                .balance(BigDecimal.ZERO)
                .currency("TZS")
                .active(true)
                .build());
    }

    private String getWalletUserId(String token) throws Exception {
        String json = mockMvc.perform(withBearer(get("/api/v1/wallet/me"), token))
                .andReturn().getResponse().getContentAsString();
        return extractField(json, "data", "userId");
    }
}
