CREATE TABLE accounts (
    account_id VARCHAR(100) PRIMARY KEY,
    balance NUMERIC(19, 4) NOT NULL CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ledger_entry_type CHECK (type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_ledger_entries_payment_id ON ledger_entries (payment_id);

CREATE TABLE processed_payments (
    payment_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

INSERT INTO accounts (account_id, balance, currency) VALUES
    ('account-001', 1000.0000, 'USD'),
    ('vendor-001', 0.0000, 'USD')
ON CONFLICT (account_id) DO NOTHING;
