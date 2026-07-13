CREATE TABLE telegram_processed_updates (
    update_id BIGINT PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processing_started_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL,
    handler_key VARCHAR(100),
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    response_fingerprint VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_telegram_processed_updates_id CHECK (update_id >= 0),
    CONSTRAINT chk_telegram_processed_updates_attempts CHECK (attempt_count >= 0),
    CONSTRAINT chk_telegram_processed_updates_status CHECK (
        status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD')
    ),
    CONSTRAINT chk_telegram_processed_updates_processing_time CHECK (
        (status = 'PROCESSING' AND processing_started_at IS NOT NULL)
        OR status <> 'PROCESSING'
    ),
    CONSTRAINT chk_telegram_processed_updates_processed_time CHECK (
        (status = 'PROCESSED' AND processed_at IS NOT NULL)
        OR status <> 'PROCESSED'
    ),
    CONSTRAINT chk_telegram_processed_updates_failed_time CHECK (
        (status IN ('FAILED', 'DEAD') AND failed_at IS NOT NULL)
        OR status NOT IN ('FAILED', 'DEAD')
    )
);

CREATE INDEX idx_telegram_processed_updates_status ON telegram_processed_updates (status);
CREATE INDEX idx_telegram_processed_updates_received_at ON telegram_processed_updates (received_at);
CREATE INDEX idx_telegram_processed_updates_processed_at ON telegram_processed_updates (processed_at);

CREATE TABLE telegram_polling_state (
    bot_identity VARCHAR(100) PRIMARY KEY,
    next_offset BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_telegram_polling_state_offset CHECK (next_offset >= 0),
    CONSTRAINT chk_telegram_polling_state_identity CHECK (btrim(bot_identity) <> '')
);

CREATE TABLE telegram_sensitive_actions (
    id UUID PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    subscription_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    result_fingerprint VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_telegram_sensitive_actions_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id),
    CONSTRAINT chk_telegram_sensitive_actions_user CHECK (telegram_user_id > 0),
    CONSTRAINT chk_telegram_sensitive_actions_type CHECK (type IN ('ROTATE_SUBSCRIPTION_TOKEN')),
    CONSTRAINT chk_telegram_sensitive_actions_status CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_telegram_sensitive_actions_completed_time CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR status <> 'COMPLETED'
    ),
    CONSTRAINT chk_telegram_sensitive_actions_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_telegram_sensitive_actions_user ON telegram_sensitive_actions (telegram_user_id);
CREATE INDEX idx_telegram_sensitive_actions_subscription ON telegram_sensitive_actions (subscription_id);
CREATE INDEX idx_telegram_sensitive_actions_status ON telegram_sensitive_actions (status);
CREATE INDEX idx_telegram_sensitive_actions_expires_at ON telegram_sensitive_actions (expires_at);
