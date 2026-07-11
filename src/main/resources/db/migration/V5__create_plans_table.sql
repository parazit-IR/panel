CREATE TABLE plans (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL,
    price_amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    duration_days INTEGER NOT NULL,
    traffic_limit_bytes BIGINT,
    max_devices INTEGER,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_plans_code UNIQUE (code),
    CONSTRAINT chk_plans_code_format CHECK (code ~ '^[A-Z0-9_-]{3,64}$'),
    CONSTRAINT chk_plans_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT chk_plans_description_not_blank CHECK (description IS NULL OR btrim(description) <> ''),
    CONSTRAINT chk_plans_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_plans_type CHECK (type IN ('TRAFFIC_LIMITED', 'UNLIMITED')),
    CONSTRAINT chk_plans_price_amount CHECK (price_amount >= 0),
    CONSTRAINT chk_plans_currency CHECK (currency IN ('IRT')),
    CONSTRAINT chk_plans_duration_days CHECK (duration_days > 0),
    CONSTRAINT chk_plans_traffic_consistency CHECK (
        (
            type = 'UNLIMITED'
            AND traffic_limit_bytes IS NULL
        )
        OR
        (
            type = 'TRAFFIC_LIMITED'
            AND traffic_limit_bytes IS NOT NULL
            AND traffic_limit_bytes > 0
        )
    ),
    CONSTRAINT chk_plans_max_devices CHECK (max_devices IS NULL OR max_devices > 0),
    CONSTRAINT chk_plans_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_plans_code ON plans (code);
CREATE INDEX idx_plans_status ON plans (status);
CREATE INDEX idx_plans_type ON plans (type);
CREATE INDEX idx_plans_display_order ON plans (display_order);
CREATE INDEX idx_plans_created_at ON plans (created_at);
