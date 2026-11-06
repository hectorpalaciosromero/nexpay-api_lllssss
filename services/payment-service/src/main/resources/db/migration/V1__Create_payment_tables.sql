-- Payment Service: Initial schema

CREATE TABLE IF NOT EXISTS payments (
    id                  UUID            PRIMARY KEY,
    idempotency_key     VARCHAR(255)    NOT NULL UNIQUE,
    sender_wallet_id    UUID            NOT NULL,
    receiver_wallet_id  UUID            NOT NULL,
    amount              NUMERIC(19,4)   NOT NULL,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'USD',
    description         VARCHAR(500),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(1000),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP
);

CREATE UNIQUE INDEX idx_payment_idempotency ON payments(idempotency_key);
CREATE INDEX idx_payment_sender             ON payments(sender_wallet_id);
CREATE INDEX idx_payment_receiver           ON payments(receiver_wallet_id);
CREATE INDEX idx_payment_status             ON payments(status);
CREATE INDEX idx_payment_created            ON payments(created_at DESC);

COMMENT ON TABLE payments IS 'P2P payment transactions with saga state tracking';
