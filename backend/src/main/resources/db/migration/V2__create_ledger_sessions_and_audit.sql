CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY,
    wallet_id UUID UNIQUE REFERENCES wallets(id),
    purpose VARCHAR(24) NOT NULL CHECK (purpose IN ('WALLET', 'SANDBOX_ISSUANCE')),
    currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency = 'USD'),
    CHECK ((purpose = 'WALLET') = (wallet_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_sandbox_issuance ON ledger_accounts(purpose) WHERE purpose = 'SANDBOX_ISSUANCE';
INSERT INTO ledger_accounts(id, purpose) VALUES ('00000000-0000-0000-0000-000000000001', 'SANDBOX_ISSUANCE');

CREATE TABLE journal_operations (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    kind VARCHAR(24) NOT NULL CHECK (kind IN ('SANDBOX_GRANT', 'TRANSFER')),
    sender_wallet_id UUID REFERENCES wallets(id),
    receiver_wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0 AND amount <> 'NaN'::numeric AND amount = trunc(amount, 2)),
    currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency = 'USD'),
    description VARCHAR(240) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((kind = 'TRANSFER') = (sender_wallet_id IS NOT NULL)),
    CHECK (sender_wallet_id <> receiver_wallet_id)
);
CREATE INDEX ix_journal_sender_date ON journal_operations(sender_wallet_id, created_at DESC, id DESC);
CREATE INDEX ix_journal_receiver_date ON journal_operations(receiver_wallet_id, created_at DESC, id DESC);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL REFERENCES journal_operations(id),
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    entry_type VARCHAR(6) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0 AND amount <> 'NaN'::numeric AND amount = trunc(amount, 2)),
    currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency = 'USD'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (operation_id, entry_type)
);
CREATE INDEX ix_ledger_account ON ledger_entries(account_id);

CREATE FUNCTION reject_financial_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Financial records are immutable' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER immutable_journal BEFORE UPDATE OR DELETE ON journal_operations
    FOR EACH ROW EXECUTE FUNCTION reject_financial_mutation();
CREATE TRIGGER immutable_entries BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION reject_financial_mutation();
CREATE TRIGGER immutable_accounts BEFORE UPDATE OR DELETE ON ledger_accounts
    FOR EACH ROW EXECUTE FUNCTION reject_financial_mutation();

CREATE FUNCTION verify_balanced_operation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE op journal_operations; op_id UUID; valid_entries INTEGER;
BEGIN
    IF TG_TABLE_NAME = 'journal_operations' THEN op_id := NEW.id; ELSE op_id := NEW.operation_id; END IF;
    SELECT * INTO STRICT op FROM journal_operations WHERE id = op_id;
    SELECT count(*) INTO valid_entries
    FROM ledger_entries e JOIN ledger_accounts a ON a.id = e.account_id
    WHERE e.operation_id = op_id AND e.amount = op.amount AND e.currency = op.currency
      AND a.currency = op.currency AND (
        (e.entry_type = 'CREDIT' AND a.wallet_id = op.receiver_wallet_id) OR
        (e.entry_type = 'DEBIT' AND (
            (op.kind = 'TRANSFER' AND a.wallet_id = op.sender_wallet_id) OR
            (op.kind = 'SANDBOX_GRANT' AND a.purpose = 'SANDBOX_ISSUANCE'))));
    IF valid_entries <> 2 THEN
        RAISE EXCEPTION 'A journal operation requires matching debit and credit entries' USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$;
CREATE CONSTRAINT TRIGGER balanced_journal AFTER INSERT ON journal_operations
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verify_balanced_operation();
CREATE CONSTRAINT TRIGGER balanced_entries AFTER INSERT ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION verify_balanced_operation();

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CHECK (expires_at > created_at)
);
CREATE INDEX ix_sessions_user ON auth_sessions(user_id);
CREATE TABLE refresh_tokens (
    token_hash CHAR(64) PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES auth_sessions(id),
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_refresh_session ON refresh_tokens(session_id);

CREATE TABLE transfer_requests (
    user_id UUID NOT NULL REFERENCES users(id),
    idempotency_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    operation_id UUID REFERENCES journal_operations(id),
    response_body TEXT,
    response_status SMALLINT,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, idempotency_key),
    CHECK ((operation_id IS NULL AND response_body IS NULL AND response_status IS NULL)
        OR (operation_id IS NOT NULL AND response_body IS NOT NULL AND response_status = 201))
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    event_type VARCHAR(48) NOT NULL,
    actor_user_id UUID REFERENCES users(id),
    entity_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION reject_financial_mutation();
