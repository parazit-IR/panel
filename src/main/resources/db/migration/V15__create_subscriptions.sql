CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    plan_selection_id UUID NOT NULL,
    xui_client_provision_id UUID NOT NULL,
    inbound_id BIGINT NOT NULL,
    remote_client_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    access_token_hash VARCHAR(64) NOT NULL,
    access_token_prefix VARCHAR(20) NOT NULL,
    token_version INTEGER NOT NULL,
    activated_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    last_accessed_at TIMESTAMPTZ,
    access_count BIGINT NOT NULL,
    revoke_reason VARCHAR(500),
    display_name VARCHAR(200),
    content_version VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_subscriptions_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_subscriptions_plan_selection FOREIGN KEY (plan_selection_id) REFERENCES plan_selections (id),
    CONSTRAINT fk_subscriptions_xui_client_provision FOREIGN KEY (xui_client_provision_id) REFERENCES xui_client_provisions (id),
    CONSTRAINT uq_subscriptions_xui_client_provision UNIQUE (xui_client_provision_id),
    CONSTRAINT uq_subscriptions_access_token_hash UNIQUE (access_token_hash),
    CONSTRAINT chk_subscriptions_inbound_id CHECK (inbound_id > 0),
    CONSTRAINT chk_subscriptions_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED', 'INVALID')
    ),
    CONSTRAINT chk_subscriptions_token_hash CHECK (access_token_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT chk_subscriptions_token_prefix CHECK (btrim(access_token_prefix) <> ''),
    CONSTRAINT chk_subscriptions_token_version CHECK (token_version > 0),
    CONSTRAINT chk_subscriptions_access_count CHECK (access_count >= 0),
    CONSTRAINT chk_subscriptions_active_activated CHECK (
        (status = 'ACTIVE' AND activated_at IS NOT NULL)
        OR
        (status <> 'ACTIVE')
    ),
    CONSTRAINT chk_subscriptions_revoked_at CHECK (
        (status = 'REVOKED' AND revoked_at IS NOT NULL)
        OR
        (status <> 'REVOKED')
    )
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_order_id ON subscriptions (order_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);
CREATE INDEX idx_subscriptions_expires_at ON subscriptions (expires_at);
CREATE INDEX idx_subscriptions_access_token_prefix ON subscriptions (access_token_prefix);
CREATE INDEX idx_subscriptions_remote_client_id ON subscriptions (remote_client_id);
