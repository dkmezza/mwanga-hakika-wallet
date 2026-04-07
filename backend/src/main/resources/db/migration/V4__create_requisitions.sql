-- ============================================================
-- V4 — Top-up requisitions table
-- ============================================================
CREATE TABLE top_up_requisitions (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL REFERENCES users (id),
    wallet_id           UUID            NOT NULL REFERENCES wallets (id),
    requested_amount    NUMERIC(19, 4)  NOT NULL CHECK (requested_amount > 0),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    note                VARCHAR(500),       -- user's optional note / payment reference
    admin_note          VARCHAR(500),       -- admin's decision note
    reviewed_by         UUID            REFERENCES users (id),
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_req_user_id  ON top_up_requisitions (user_id);
CREATE INDEX idx_req_status   ON top_up_requisitions (status);
CREATE INDEX idx_req_created  ON top_up_requisitions (created_at DESC);

COMMENT ON TABLE  top_up_requisitions               IS 'User requests for admin-initiated wallet top-ups';
COMMENT ON COLUMN top_up_requisitions.status        IS 'PENDING → APPROVED (wallet credited) | REJECTED';
COMMENT ON COLUMN top_up_requisitions.reviewed_by   IS 'Admin who actioned the request';
COMMENT ON COLUMN top_up_requisitions.reviewed_at   IS 'NULL while PENDING';
