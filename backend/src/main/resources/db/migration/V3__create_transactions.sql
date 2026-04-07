-- ============================================================
-- V3 — Transactions table (append-only financial ledger)
-- ============================================================
CREATE TABLE transactions (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    reference           VARCHAR(255)    NOT NULL,            -- idempotency key
    type                VARCHAR(20)     NOT NULL CHECK (type IN ('TOP_UP', 'TRANSFER')),
    status              VARCHAR(20)     NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    sender_wallet_id    UUID            REFERENCES wallets (id),   -- NULL for TOP_UP
    receiver_wallet_id  UUID            NOT NULL REFERENCES wallets (id),
    amount              NUMERIC(19, 4)  NOT NULL CHECK (amount > 0),
    fee                 NUMERIC(19, 4)  NOT NULL DEFAULT 0.0000,
    description         VARCHAR(500),
    initiated_by        UUID            NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tx_reference UNIQUE (reference),
    CONSTRAINT chk_tx_fee_non_neg CHECK (fee >= 0)
);

-- Indexes optimised for the most common query patterns
CREATE INDEX idx_tx_reference       ON transactions (reference);
CREATE INDEX idx_tx_sender_wallet   ON transactions (sender_wallet_id);
CREATE INDEX idx_tx_receiver_wallet ON transactions (receiver_wallet_id);
CREATE INDEX idx_tx_initiated_by    ON transactions (initiated_by);
CREATE INDEX idx_tx_created_at      ON transactions (created_at DESC);

COMMENT ON TABLE  transactions               IS 'Immutable financial ledger — rows are never updated';
COMMENT ON COLUMN transactions.reference     IS 'Client idempotency key; unique per transaction';
COMMENT ON COLUMN transactions.sender_wallet_id IS 'NULL for TOP_UP transactions';
