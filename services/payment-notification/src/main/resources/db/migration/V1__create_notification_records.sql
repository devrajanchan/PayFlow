CREATE TABLE notification_records (
    payment_id UUID PRIMARY KEY,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
