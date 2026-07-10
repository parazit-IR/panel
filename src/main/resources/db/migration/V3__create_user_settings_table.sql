CREATE TABLE user_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    renewal_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    usage_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    usage_alert_threshold_percent INTEGER NOT NULL DEFAULT 80,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_settings_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_settings_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_settings_usage_alert_threshold_percent
        CHECK (usage_alert_threshold_percent BETWEEN 1 AND 100)
);

CREATE INDEX idx_user_settings_user_id ON user_settings (user_id);
