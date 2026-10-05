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
