CREATE TABLE scheduled_transfers (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_email VARCHAR(254) NOT NULL,
    amount NUMERIC(17,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    description VARCHAR(240) NOT NULL DEFAULT '',
    reference VARCHAR(80),
    execute_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    operation_id UUID REFERENCES journal_operations(id),
    failure_code VARCHAR(80),
    failure_message VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (amount > 0),
    CHECK (currency = 'USD'),
    CHECK (status IN ('SCHEDULED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX ix_scheduled_transfers_due
    ON scheduled_transfers(execute_at, id)
    WHERE status = 'SCHEDULED';

CREATE INDEX ix_scheduled_transfers_processing
    ON scheduled_transfers(updated_at, id)
    WHERE status = 'PROCESSING';

CREATE INDEX ix_scheduled_transfers_user
    ON scheduled_transfers(user_id, created_at DESC);
