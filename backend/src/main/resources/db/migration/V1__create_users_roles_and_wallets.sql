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

-- Opening funds must be posted through the ledger once registration is implemented.
-- A newly persisted wallet starts at zero; there is no unrecorded balance grant here.
