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
