package com.mwanga.wallet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Swagger UI and the OpenAPI spec served at /api-docs.
 *
 * <p>The "bearerAuth" security scheme defined here is referenced by
 * {@code @SecurityRequirement(name = "bearerAuth")} on protected controllers,
 * which causes Swagger UI to show the "Authorize" padlock for those endpoints.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Mwanga Hakika Bank — Digital Wallet API",
                description = """
                        RESTful API for the Mwanga Hakika Bank digital wallet service.

                        **Roles**
                        - `ADMIN` — top-up wallets, approve/reject top-up requests, manage users.
                        - `USER`  — view balance, transfer funds, submit top-up requests.

                        **Authentication** — all protected endpoints require a Bearer JWT token.
                        Obtain one via `POST /api/v1/auth/login`.
                        """,
                version = "v1.0.0",
                contact = @Contact(
                        name = "Mwanga Innovation Team",
                        email = "innovation@mwanga.co.tz"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "http://backend:8080",  description = "Docker Compose")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT access token. Format: `Bearer <token>`",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
