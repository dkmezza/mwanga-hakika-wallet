-- ============================================================
-- V1 — Users table
-- ============================================================
-- UUID generation requires pgcrypto (gen_random_uuid).
-- Available natively in PostgreSQL 13+; kept explicit for clarity.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    full_name   VARCHAR(100)    NOT NULL,
    role        VARCHAR(20)     NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);

COMMENT ON TABLE  users              IS 'Platform user accounts';
COMMENT ON COLUMN users.role        IS 'ADMIN: full access | USER: wallet operations only';
COMMENT ON COLUMN users.is_active   IS 'FALSE = soft-deleted / suspended account';
