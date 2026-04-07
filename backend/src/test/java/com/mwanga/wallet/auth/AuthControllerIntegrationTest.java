package com.mwanga.wallet.auth;

import com.mwanga.wallet.BaseIntegrationTest;
import com.mwanga.wallet.auth.dto.LoginRequest;
import com.mwanga.wallet.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth endpoints — integration")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ── POST /api/v1/auth/register ─────────────────────────────────────────────

    @Test
    @DisplayName("register: valid payload → 201 with access and refresh tokens")
    void register_validPayload_returns201WithTokens() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Integration User");
        req.setEmail("integration-register-1@test.com");
        req.setPassword("Password@123");

        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andReturn().getResponse().getContentAsString();

        String email = extractField(body, "data", "email");
        assertThat(email).isEqualTo("integration-register-1@test.com");
    }

    @Test
    @DisplayName("register: duplicate email → 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Dup User");
        req.setEmail("integration-dup@test.com");
        req.setPassword("Password@123");

        // First registration — must succeed
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Second registration with same email — must be rejected
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("register: invalid email format → 400 with validation errors")
    void register_invalidEmailFormat_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Bad Email");
        req.setEmail("not-an-email");
        req.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.email").isNotEmpty());
    }

    @Test
    @DisplayName("register: password too short → 400 with validation error")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Short Pass");
        req.setEmail("shortpass@test.com");
        req.setPassword("short");   // < 8 chars

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").isNotEmpty());
    }

    // ── POST /api/v1/auth/login ────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials → 200 with tokens")
    void login_validCredentials_returns200WithTokens() throws Exception {
        // Register first
        registerAndGetToken("Login User", "integration-login-1@test.com", "Password@123");

        LoginRequest req = new LoginRequest();
        req.setEmail("integration-login-1@test.com");
        req.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("integration-login-1@test.com"));
    }

    @Test
    @DisplayName("login: wrong password → 401 Unauthorized with no token leak")
    void login_wrongPassword_returns401() throws Exception {
        registerAndGetToken("Wrong Pass", "integration-login-2@test.com", "Password@123");

        LoginRequest req = new LoginRequest();
        req.setEmail("integration-login-2@test.com");
        req.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                // Critically: error message must NOT reveal whether email or password was wrong
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("login: unknown email → 401 (same response as wrong password — no user enumeration)")
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@test.com");
        req.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
