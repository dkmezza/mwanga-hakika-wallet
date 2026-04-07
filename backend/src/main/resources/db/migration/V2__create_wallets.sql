-- ============================================================
-- V2 — Wallets table
-- ============================================================
CREATE TABLE wallets (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL,
    balance     NUMERIC(19, 4)  NOT NULL DEFAULT 0.0000,
    currency    VARCHAR(3)      NOT NULL DEFAULT 'TZS',
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    version     BIGINT          NOT NULL DEFAULT 0,   -- JPA optimistic locking
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_wallets_user    FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_wallets_user_id UNIQUE (user_id),           -- one wallet per user
    CONSTRAINT chk_balance_non_neg CHECK (balance >= 0)       -- DB-level safety net
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);

COMMENT ON TABLE  wallets           IS 'User financial accounts — one per user';
COMMENT ON COLUMN wallets.balance   IS 'NUMERIC(19,4): never use FLOAT/DOUBLE for money';
COMMENT ON COLUMN wallets.version   IS 'Hibernate @Version field — incremented on each UPDATE';
COMMENT ON COLUMN wallets.currency  IS 'ISO 4217 code, e.g. TZS';
