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
