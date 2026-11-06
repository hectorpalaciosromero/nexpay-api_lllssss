-- Wallet Service: Initial schema

CREATE TABLE IF NOT EXISTS wallets (
    id              UUID            PRIMARY KEY,
    owner_email     VARCHAR(255)    NOT NULL UNIQUE,
    owner_name      VARCHAR(100)    NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'USD',
    balance         NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE INDEX idx_wallet_email ON wallets(owner_email);
CREATE INDEX idx_wallet_status ON wallets(status);

CREATE TABLE IF NOT EXISTS movements (
    id              UUID            PRIMARY KEY,
    wallet_id       UUID            NOT NULL REFERENCES wallets(id),
    type            VARCHAR(20)     NOT NULL,
    amount          NUMERIC(19,4)   NOT NULL,
    balance_after   NUMERIC(19,4)   NOT NULL,
    description     VARCHAR(500),
    reference_id    VARCHAR(100),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_movement_wallet_id ON movements(wallet_id);
CREATE INDEX idx_movement_wallet_created ON movements(wallet_id, created_at DESC);
CREATE INDEX idx_movement_reference ON movements(reference_id);

COMMENT ON TABLE wallets IS 'Digital wallets holding user balances';
COMMENT ON TABLE movements IS 'Financial movement history for each wallet';
