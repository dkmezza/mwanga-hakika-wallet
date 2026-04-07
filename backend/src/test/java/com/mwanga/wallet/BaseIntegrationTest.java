package com.mwanga.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mwanga.wallet.auth.dto.LoginRequest;
import com.mwanga.wallet.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for all integration tests.
 *
 * <p>A single PostgreSQL container is started once per JVM (static), shared across all
 * subclasses. Flyway runs all four migrations automatically on startup.
 * The DataSeeder is excluded via {@code @Profile("!test")} — each test class
 * creates its own data, typically using unique email addresses to avoid conflicts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("mwanga_wallet_test")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── Auth helpers ───────────────────────────────────────────────────────────

    /**
     * Registers a USER account and returns the access token.
     */
    protected String registerAndGetToken(String fullName, String email, String password)
            throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName(fullName);
        req.setEmail(email);
        req.setPassword(password);

        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return extractField(body, "data", "accessToken");
    }

    /**
     * Logs in with email + password and returns the access token.
     */
    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return extractField(body, "data", "accessToken");
    }

    // ── Request builders ───────────────────────────────────────────────────────

    protected MockHttpServletRequestBuilder withBearer(
            MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    // ── JSON helpers ───────────────────────────────────────────────────────────

    protected String extractField(String json, String... path) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        for (String key : path) {
            node = node.path(key);
        }
        return node.asText();
    }

    protected JsonNode extractNode(String json, String... path) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        for (String key : path) {
            node = node.path(key);
        }
        return node;
    }
}
