CREATE TABLE xui_client_provisions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_selection_id UUID NOT NULL,
    inbound_id BIGINT NOT NULL,
    remote_client_id VARCHAR(64) NOT NULL,
    remote_email VARCHAR(128) NOT NULL,
    remote_subscription_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    traffic_limit_bytes BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    ip_limit INTEGER NOT NULL,
    provisioned_at TIMESTAMPTZ,
    last_synchronized_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_xui_client_provisions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_xui_client_provisions_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT fk_xui_client_provisions_plan_selection FOREIGN KEY (plan_selection_id) REFERENCES plan_selections (id),
    CONSTRAINT uq_xui_client_provisions_plan_selection UNIQUE (plan_selection_id),
    CONSTRAINT uq_xui_client_provisions_remote_client UNIQUE (remote_client_id),
    CONSTRAINT uq_xui_client_provisions_remote_email UNIQUE (remote_email),
    CONSTRAINT chk_xui_client_provisions_inbound_id CHECK (inbound_id > 0),
    CONSTRAINT chk_xui_client_provisions_remote_client_uuid CHECK (
        remote_client_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    ),
    CONSTRAINT chk_xui_client_provisions_remote_email CHECK (btrim(remote_email) <> ''),
    CONSTRAINT chk_xui_client_provisions_status CHECK (status IN ('PENDING', 'PROVISIONING', 'ACTIVE', 'FAILED', 'UNKNOWN')),
    CONSTRAINT chk_xui_client_provisions_traffic CHECK (traffic_limit_bytes >= 0),
    CONSTRAINT chk_xui_client_provisions_ip_limit CHECK (ip_limit >= 0),
    CONSTRAINT chk_xui_client_provisions_active_provisioned CHECK (
        (status = 'ACTIVE' AND provisioned_at IS NOT NULL)
        OR
        (status <> 'ACTIVE')
    )
);

CREATE INDEX idx_xui_client_provisions_user_id ON xui_client_provisions (user_id);
CREATE INDEX idx_xui_client_provisions_plan_id ON xui_client_provisions (plan_id);
CREATE INDEX idx_xui_client_provisions_plan_selection_id ON xui_client_provisions (plan_selection_id);
CREATE INDEX idx_xui_client_provisions_inbound_id ON xui_client_provisions (inbound_id);
CREATE INDEX idx_xui_client_provisions_status ON xui_client_provisions (status);
CREATE INDEX idx_xui_client_provisions_expires_at ON xui_client_provisions (expires_at);
CREATE INDEX idx_xui_client_provisions_remote_client_id ON xui_client_provisions (remote_client_id);
CREATE INDEX idx_xui_client_provisions_remote_email ON xui_client_provisions (remote_email);
