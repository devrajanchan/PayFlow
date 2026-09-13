CREATE TABLE payments (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    source_account_id VARCHAR(100) NOT NULL,
    destination_account_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_payments_client_idempotency UNIQUE (client_id, idempotency_key)
);

CREATE INDEX idx_payments_status_created_at ON payments (status, created_at);
