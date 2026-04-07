# Mwanga Hakika Bank — Digital Wallet Service

A production-quality backend service for a digital wallet, built as a technical take-home task for the **Software Developer (Innovation)** position at Mwanga Hakika Bank.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Quick Start (Docker)](#quick-start-docker)
5. [Local Development (without Docker)](#local-development-without-docker)
6. [Running Tests](#running-tests)
7. [API Reference](#api-reference)
8. [Seeded Accounts](#seeded-accounts)
9. [Key Design Decisions](#key-design-decisions)
10. [Assumptions](#assumptions)
11. [What I Would Do Next](#what-i-would-do-next)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    docker-compose                        │
│                                                          │
│  Browser / Mobile                                        │
│       │                                                  │
│       ▼                                                  │
│  Next.js :3000  ──────────────────────────────────────► │
│                                                          │
│  Flutter (native)  ───────────────────────────────────► │
│                          │                               │
│                          ▼                               │
│                  Spring Boot :8080                       │
│                   (REST + JWT)                           │
│                          │                               │
│                          ▼                               │
│                   PostgreSQL :5432                       │
│                 (Flyway migrations)                      │
└─────────────────────────────────────────────────────────┘
```

All services run as Docker containers orchestrated by a single `docker-compose.yml`.  
Flyway applies all schema migrations automatically on startup — no manual SQL steps needed.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (LTS) |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security 6 + JWT (JJWT 0.12.x) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate 6 |
| Migrations | Flyway |
| API Docs | springdoc-openapi (Swagger UI) |
| Rate Limiting | Bucket4j |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Frontend | Next.js 14 (App Router, TypeScript, TanStack Query) |
| Mobile | Flutter (Riverpod) |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## Project Structure

```
mwanga-wallet/
├── backend/                         # Spring Boot API
│   ├── src/main/java/com/mwanga/wallet/
│   │   ├── auth/                    # Login, register, JWT refresh
│   │   ├── user/                    # User management
│   │   ├── wallet/                  # Balance, admin top-up
│   │   ├── transaction/             # Transfers + transaction history
│   │   ├── requisition/             # User top-up requests + admin workflow
│   │   ├── security/                # JWT filter, Spring Security config
│   │   ├── common/                  # Shared DTOs, exceptions, base entity
│   │   └── config/                  # Swagger, auditing, data seeder
│   └── src/main/resources/
│       └── db/migration/            # Flyway V1–V4 SQL migrations
├── frontend/                        # Next.js 14 web app
├── mobile/                          # Flutter mobile app
├── docker-compose.yml
├── .env.example
└── .github/workflows/ci.yml
```

The backend follows a **package-by-feature** structure. Each domain (auth, wallet, transaction, requisition) owns its controller, service, repository, entity, and DTOs — not split across horizontal layers.

---

## Quick Start (Docker)

### Prerequisites
- Docker Desktop ≥ 24 (with Compose v2)
- 4 GB RAM available to Docker

```bash
# 1. Clone the repository
git clone https://github.com/dkmezza/mwanga-hakika-wallet
cd mwanga-hakika-wallet

# 2. Create your environment file
cp .env.example .env

# 3. Set a secure JWT secret (required)
#    Generate one with:
openssl rand -base64 64
#    Then paste it as JWT_SECRET in .env

# 4. Start all services
#
# IMPORTANT — if you have run Docker Compose before (even with different credentials),
# the postgres_data volume may already exist with the old user/password baked in.
# PostgreSQL will skip re-initialization and the role "postgres" will not exist,
# causing authentication failures. To start clean, wipe the volume first:
#
#   docker compose down -v   # removes containers AND the postgres_data volume
#
# Then run:
docker compose up --build

# Services will be available at:
#   Backend API  → http://localhost:8080
#   Swagger UI   → http://localhost:8080/swagger-ui.html
#   Frontend     → http://localhost:3000
#   PostgreSQL   → localhost:5432

# 5. Run the Flutter mobile app
cd mobile
flutter pub get
flutter run

```

The first startup takes ~2 minutes as Maven downloads dependencies and builds the JAR.  
Subsequent starts use the Docker layer cache and take ~30 seconds.

### Stopping

```bash
docker compose down        # stop containers, keep database volume
docker compose down -v     # stop containers AND delete the database volume
                           # (required if you change DB_USERNAME or DB_PASSWORD,
                           #  or if you see "role does not exist" errors on startup)
```

---

## Local Development (without Docker)

### Prerequisites
- Java 21 (`java -version`)
- Maven 3.9+ OR use the included `./mvnw` wrapper
- PostgreSQL 16 running locally

```bash
# 1. Create the database
psql -U postgres -c "CREATE DATABASE mwanga_wallet;"

# 2. Export environment variables (or create a .env and source it)
export DB_URL=jdbc:postgresql://localhost:5432/mwanga_wallet
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=$(openssl rand -base64 64)

# 3. Run the backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# API available at http://localhost:8080
# Swagger UI at  http://localhost:8080/swagger-ui.html
```

---

## Running Tests

```bash
cd backend

# All tests (unit + integration via Testcontainers)
# Docker must be running — Testcontainers pulls postgres:16-alpine automatically
./mvnw verify

# Unit tests only (no Docker required)
./mvnw test -Dgroups="unit"

# View test reports
open target/surefire-reports/index.html   # macOS
xdg-open target/surefire-reports/         # Linux
```

### Test coverage

| Test class | Type | What it verifies |
|---|---|---|
| `AuthServiceTest` | Unit | Registration, duplicate email, login, bad credentials |
| `TransactionServiceTest` | Unit | Transfer happy path, insufficient funds, min/max limits, self-transfer, idempotency |
| `RequisitionServiceTest` | Unit | Create request, approve (credits wallet), reject (no credit), double-approve guard |
| `AuthControllerIntegrationTest` | Integration | Full HTTP register/login flow against real PostgreSQL |
| `TransactionControllerIntegrationTest` | Integration | Transfer, insufficient funds, auth guard, idempotency key deduplication |

---

## API Reference

Swagger UI (interactive): **`http://localhost:8080/swagger-ui.html`**  
OpenAPI JSON spec: `http://localhost:8080/api-docs`

### Authentication

All protected endpoints require:
```
Authorization: Bearer <access_token>
```

Obtain a token via `POST /api/v1/auth/login`.

---

### Auth endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Create a USER account + wallet |
| `POST` | `/api/v1/auth/login` | Public | Authenticate, get JWT tokens |
| `POST` | `/api/v1/auth/refresh` | Public | Refresh tokens via `X-Refresh-Token` header |

**Register**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Alice Mwangi",
    "email": "alice@example.com",
    "password": "Password@123"
  }'
```

**Login**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "password": "Password@123"}'
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer",
    "userId": "...",
    "email": "alice@example.com",
    "role": "USER"
  }
}
```

---

### Wallet endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/wallet/me` | USER / ADMIN | Get own wallet + balance |
| `GET` | `/api/v1/wallet/me/transactions` | USER / ADMIN | Own transaction history (paginated) |
| `GET` | `/api/v1/wallet/{id}` | ADMIN | Get any wallet by ID |
| `POST` | `/api/v1/wallet/{userId}/topup` | ADMIN | Credit a user's wallet |

**Admin top-up**
```bash
curl -X POST http://localhost:8080/api/v1/wallet/{userId}/topup \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "description": "Initial wallet funding",
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

### Transfer endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/transactions/transfer` | USER | Transfer funds to another wallet |
| `GET` | `/api/v1/transactions/{id}` | USER / ADMIN | Get a transaction by ID |
| `GET` | `/api/v1/transactions` | ADMIN | All transactions (paginated) |

**Transfer**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Authorization: Bearer <user_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverWalletId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "amount": 5000,
    "description": "Lunch split",
    "idempotencyKey": "unique-client-uuid-here"
  }'
```

Limits: min **100 TZS**, max **5,000,000 TZS** per transaction (configurable via env vars).

---

### Top-up Requisition endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/requisitions` | USER | Submit a top-up request |
| `GET` | `/api/v1/requisitions/me` | USER | Own requests (paginated) |
| `GET` | `/api/v1/requisitions?status=PENDING` | ADMIN | All requests, filter by status |
| `PATCH` | `/api/v1/requisitions/{id}/approve` | ADMIN | Approve → credits wallet |
| `PATCH` | `/api/v1/requisitions/{id}/reject` | ADMIN | Reject → no wallet change |

**Submit top-up request**
```bash
curl -X POST http://localhost:8080/api/v1/requisitions \
  -H "Authorization: Bearer <user_token>" \
  -H "Content-Type: application/json" \
  -d '{"amount": 20000, "note": "Bank transfer ref: TZB-12345"}'
```

**Admin approve**
```bash
curl -X PATCH http://localhost:8080/api/v1/requisitions/{id}/approve \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{"adminNote": "Payment confirmed via bank reference"}'
```

---

### User management (Admin)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | USER / ADMIN | Own profile |
| `GET` | `/api/v1/users` | ADMIN | List all users (paginated) |
| `GET` | `/api/v1/users/{id}` | ADMIN | Get user by ID |
| `PATCH` | `/api/v1/users/{id}/activate?active=false` | ADMIN | Suspend / restore account |

---

### Pagination

All list endpoints accept `?page=0&size=20`. Response shape:
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  }
}
```

---

## Seeded Accounts

The `DataSeeder` runs on startup (skipped in the `test` profile) and creates:

| Role | Email | Password | Balance |
|---|---|---|---|
| ADMIN | `admin@mwanga.co.tz` | `Admin@1234` | 0 TZS |
| USER | `alice@example.com` | `User@1234` | 50,000 TZS |
| USER | `bob@example.com` | `User@1234` | 25,000 TZS |

> **Change these credentials before any public-facing deployment.**

---

## Key Design Decisions

### 1. `NUMERIC(19,4)` for all monetary values — never `FLOAT` or `DOUBLE`
Floating-point types cannot represent most decimal fractions exactly (e.g., `0.1 + 0.2 ≠ 0.3`). For financial data this causes incorrect balances and audit mismatches. `NUMERIC(19,4)` stores values exactly and is used end-to-end in Java as `BigDecimal`.

### 2. Optimistic locking + pessimistic write locks for concurrent transfers
The `Wallet` entity has a `@Version` field for Hibernate optimistic locking as a first layer of defence. During transfers, both wallets are additionally locked with `SELECT … FOR UPDATE` (pessimistic write lock) to guarantee atomic balance changes under high concurrency. Wallets are **always locked in ascending UUID order** to prevent the classic AB-BA deadlock.

### 3. Append-only transaction ledger
`Transaction` rows are never `UPDATE`d after creation. This creates an immutable audit trail — every credit and debit is permanently recorded. The entity intentionally has no `updatedAt` field to make this contract explicit.

### 4. Idempotency keys on transfer and top-up
Every transfer and top-up accepts an optional `idempotencyKey`. If a request is retried with the same key (e.g., due to a network timeout), the API returns the original transaction instead of creating a duplicate charge. Keys are stored as a `UNIQUE` constraint on the `transactions.reference` column.

### 5. Package-by-feature structure
The Spring Boot backend is organised by domain (`auth`, `wallet`, `transaction`, `requisition`) rather than by layer (`controllers`, `services`, `repositories`). Each feature is self-contained and can be reasoned about in isolation. Cross-cutting concerns (security, common DTOs, exception handling) live in dedicated `security`, `common`, and `config` packages.

### 6. JWT stateless authentication
No server-side sessions. Every request carries a signed JWT. Spring Security validates it in `JwtAuthFilter` before the request reaches any controller. Role claims are embedded in the token, enabling `@PreAuthorize("hasRole('ADMIN')")` method-level security.

### 7. Flyway for schema migrations
Flyway owns the schema — Hibernate is configured with `ddl-auto: validate` so it only verifies entity-table alignment but never alters tables. This is the only safe approach in a production system where schema changes must be versioned, reviewed, and applied consistently across environments.

### 8. Safe error responses
`GlobalExceptionHandler` maps every exception to a safe JSON response. Stack traces, exception class names, and internal error messages are **never sent to the client**. Failed login attempts return `"Invalid email or password"` regardless of whether the email or password was wrong — preventing user enumeration.

### 9. Configuration via environment variables
All secrets and tunable parameters (`JWT_SECRET`, `DB_PASSWORD`, `TRANSFER_MIN_AMOUNT`, etc.) are injected via environment variables with sensible development defaults in `application.yml`. The `.env.example` documents every variable.

### 10. Monorepo
Backend, frontend, and mobile live in one repository to simplify the Docker Compose orchestration, share a single CI pipeline, and give reviewers a complete view of the system in one clone.

---

## Assumptions

1. **One wallet per user, TZS only.** The system supports a single wallet per user in Tanzanian Shillings. Multi-currency and multiple wallets per user would require a more complex transfer model.

2. **Top-up requisitions are informational.** The workflow captures a user's request and lets an admin approve/reject it. No integration with an external payment gateway is implemented — the assumption is that the admin verifies the payment offline (bank transfer reference, etc.) and then approves in the system.

3. **No fees.** The `fee` column exists on the `Transaction` table to support future monetisation, but all transactions currently have `fee = 0`.

4. **Refresh tokens are stateless JWTs.** Refresh tokens are signed JWTs stored on the client, not in the database. This means individual tokens cannot be revoked server-side before expiry. A production system would store refresh tokens in the database with a revocation flag.

5. **Admin accounts are seeded.** The public `/register` endpoint always creates `USER`-role accounts. Admin accounts are created by the `DataSeeder` on startup. A real system would have a protected admin-creation flow.

6. **Rate limiting is configured but minimally applied.** The `Bucket4j` dependency is included and `WalletProperties` exposes rate-limit configuration. Full per-IP / per-user rate limiting on auth and transfer endpoints would be wired in the next iteration.

---

## What I Would Do Next

Given more time, the next priorities would be:

1. **Refresh token revocation** — store refresh tokens in a `refresh_tokens` table with an `is_revoked` flag. Add a `POST /auth/logout` endpoint that revokes the current refresh token.

2. **Rate limiting on auth endpoints** — apply `Bucket4j` to `POST /auth/login` (e.g., 10 attempts per IP per minute) to prevent brute-force attacks.

3. **Database-level row-level security** — enforce that users can only read their own wallet rows at the PostgreSQL level as a defence-in-depth measure.

4. **Async notification on transfer** — publish a domain event when a transfer completes and send an in-app or SMS notification to both sender and receiver.

5. **Full Flutter app** — complete all screens (transaction history, QR code scan to pre-fill receiver wallet, biometric auth).

6. **Observability** — integrate Micrometer + Prometheus metrics and a structured JSON log format (Logstash encoder) so the service is ready to connect to a Grafana/ELK stack.

7. **Soft-delete for requisitions** — allow users to cancel their own PENDING requisitions before admin review.

8. **Pagination cursor-based** — replace offset pagination with cursor-based pagination for the transaction history endpoint, which is more efficient at scale.
