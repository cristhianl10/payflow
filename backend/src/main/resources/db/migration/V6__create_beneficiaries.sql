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
