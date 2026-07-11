CREATE TABLE plan_selections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_code_snapshot VARCHAR(64) NOT NULL,
    plan_name_snapshot VARCHAR(128) NOT NULL,
    plan_type_snapshot VARCHAR(32) NOT NULL,
    price_amount_snapshot BIGINT NOT NULL,
    currency_snapshot VARCHAR(8) NOT NULL,
    duration_days_snapshot INTEGER NOT NULL,
    traffic_limit_bytes_snapshot BIGINT,
    max_devices_snapshot INTEGER,
    status VARCHAR(32) NOT NULL,
    selected_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_plan_selections_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_plan_selections_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT chk_plan_selections_code_snapshot_format CHECK (plan_code_snapshot ~ '^[A-Z0-9_-]{3,64}$'),
    CONSTRAINT chk_plan_selections_name_snapshot_not_blank CHECK (btrim(plan_name_snapshot) <> ''),
    CONSTRAINT chk_plan_selections_status CHECK (status IN ('ACTIVE', 'CLEARED', 'EXPIRED', 'CONSUMED')),
    CONSTRAINT chk_plan_selections_plan_type_snapshot CHECK (plan_type_snapshot IN ('TRAFFIC_LIMITED', 'UNLIMITED')),
    CONSTRAINT chk_plan_selections_price_amount_snapshot CHECK (price_amount_snapshot >= 0),
    CONSTRAINT chk_plan_selections_currency_snapshot CHECK (currency_snapshot IN ('IRT')),
    CONSTRAINT chk_plan_selections_duration_days_snapshot CHECK (duration_days_snapshot > 0),
    CONSTRAINT chk_plan_selections_traffic_consistency CHECK (
        (
            plan_type_snapshot = 'UNLIMITED'
            AND traffic_limit_bytes_snapshot IS NULL
        )
        OR
        (
            plan_type_snapshot = 'TRAFFIC_LIMITED'
            AND traffic_limit_bytes_snapshot IS NOT NULL
            AND traffic_limit_bytes_snapshot > 0
        )
    ),
    CONSTRAINT chk_plan_selections_max_devices_snapshot CHECK (max_devices_snapshot IS NULL OR max_devices_snapshot > 0),
    CONSTRAINT chk_plan_selections_expires_after_selected CHECK (expires_at > selected_at)
);

CREATE INDEX idx_plan_selections_user_id ON plan_selections (user_id);
CREATE INDEX idx_plan_selections_plan_id ON plan_selections (plan_id);
CREATE INDEX idx_plan_selections_status ON plan_selections (status);
CREATE INDEX idx_plan_selections_expires_at ON plan_selections (expires_at);
CREATE INDEX idx_plan_selections_selected_at ON plan_selections (selected_at);
CREATE UNIQUE INDEX uq_plan_selections_one_active_per_user ON plan_selections (user_id)
WHERE status = 'ACTIVE';
