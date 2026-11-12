BEGIN;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL CHECK (btrim(first_name) <> ''),
    last_name VARCHAR(100) NOT NULL CHECK (btrim(last_name) <> ''),
    email VARCHAR(254) NOT NULL CHECK (email = lower(btrim(email)) AND email <> ''),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'SUSPENDED', 'CLOSED')),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE roles (
    name VARCHAR(32) PRIMARY KEY CHECK (name IN ('ROLE_USER', 'ROLE_ADMIN'))
);

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_name VARCHAR(32) NOT NULL REFERENCES roles(name),
    PRIMARY KEY (user_id, role_name)
);

CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    currency CHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency = 'USD'),
    available_balance NUMERIC(19,4) NOT NULL DEFAULT 0
        CHECK (available_balance >= 0 AND available_balance <> 'NaN'::numeric
            AND available_balance = trunc(available_balance, 2)),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wallets_user UNIQUE (user_id)
);

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

CREATE INDEX ix_transfer_requests_expiry ON transfer_requests(expires_at);

CREATE TABLE email_verification_tokens (
    token_hash CHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CHECK (expires_at > created_at)
);

CREATE INDEX ix_email_verification_tokens_user
    ON email_verification_tokens(user_id);

CREATE INDEX ix_email_verification_tokens_expiry
    ON email_verification_tokens(expires_at)
    WHERE consumed_at IS NULL;

CREATE TABLE password_reset_tokens (
    token_hash CHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CHECK (expires_at > created_at)
);

CREATE INDEX ix_password_reset_tokens_user
    ON password_reset_tokens(user_id);

CREATE INDEX ix_password_reset_tokens_expiry
    ON password_reset_tokens(expires_at)
    WHERE consumed_at IS NULL;

CREATE TABLE beneficiaries (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    beneficiary_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    alias VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_beneficiary_not_self CHECK (owner_user_id <> beneficiary_user_id),
    CONSTRAINT uq_beneficiary_owner_recipient UNIQUE (owner_user_id, beneficiary_user_id)
);

CREATE INDEX ix_beneficiaries_owner
    ON beneficiaries(owner_user_id, created_at DESC);

ALTER TABLE journal_operations
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN reference VARCHAR(80);

ALTER TABLE journal_operations
    ADD CONSTRAINT chk_journal_status
    CHECK (status IN ('COMPLETED'));

CREATE INDEX ix_journal_sender_status_date
    ON journal_operations(sender_wallet_id, status, created_at DESC)
    WHERE sender_wallet_id IS NOT NULL;

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    public_id VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(140) NOT NULL,
    message VARCHAR(400) NOT NULL,
    action_url VARCHAR(240),
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_notifications_user_created
    ON notifications(user_id, created_at DESC);

CREATE INDEX ix_notifications_unread
    ON notifications(user_id, created_at DESC)
    WHERE read_at IS NULL;

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

CREATE TABLE user_mfa (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    secret_ciphertext TEXT NOT NULL,
    enabled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mfa_recovery_codes (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash CHAR(64) NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, code_hash)
);

CREATE TABLE mfa_login_challenges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_mfa_challenge_user ON mfa_login_challenges(user_id, expires_at DESC);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon';
        EXECUTE 'REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM anon';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM anon';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM anon';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        EXECUTE 'REVOKE ALL ON ALL TABLES IN SCHEMA public FROM authenticated';
        EXECUTE 'REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM authenticated';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM authenticated';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM authenticated';
    END IF;
END $$;

COMMIT;
