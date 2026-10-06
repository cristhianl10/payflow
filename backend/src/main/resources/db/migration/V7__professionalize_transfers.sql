ALTER TABLE journal_operations
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN reference VARCHAR(80);

ALTER TABLE journal_operations
    ADD CONSTRAINT chk_journal_status
    CHECK (status IN ('COMPLETED'));

CREATE INDEX ix_journal_sender_status_date
    ON journal_operations(sender_wallet_id, status, created_at DESC)
    WHERE sender_wallet_id IS NOT NULL;
