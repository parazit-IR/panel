ALTER TABLE xui_client_provisions
    ADD COLUMN last_known_upload_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_known_download_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_known_total_bytes BIGINT NOT NULL DEFAULT 0;

ALTER TABLE xui_client_provisions
    DROP CONSTRAINT chk_xui_client_provisions_status,
    DROP CONSTRAINT chk_xui_client_provisions_active_provisioned,
    DROP CONSTRAINT chk_xui_client_provisions_disabled_at,
    DROP CONSTRAINT chk_xui_client_provisions_deleted_at,
    DROP CONSTRAINT chk_xui_client_provisions_lifecycle_timestamps;

ALTER TABLE xui_client_provisions
    ADD CONSTRAINT chk_xui_client_provisions_status CHECK (
        status IN (
            'PENDING',
            'PROVISIONING',
            'ACTIVE',
            'FAILED',
            'UNKNOWN',
            'DISABLING',
            'DISABLED',
            'ENABLING',
            'DELETING',
            'DELETED'
        )
    ),
    ADD CONSTRAINT chk_xui_client_provisions_active_provisioned CHECK (
        (status = 'ACTIVE' AND provisioned_at IS NOT NULL)
        OR
        (status <> 'ACTIVE')
    ),
    ADD CONSTRAINT chk_xui_client_provisions_disabled_at CHECK (
        (status = 'DISABLED' AND disabled_at IS NOT NULL)
        OR
        (status <> 'DISABLED')
    ),
    ADD CONSTRAINT chk_xui_client_provisions_deleted_at CHECK (
        (status = 'DELETED' AND deleted_at IS NOT NULL)
        OR
        (status <> 'DELETED')
    ),
    ADD CONSTRAINT chk_xui_client_provisions_lifecycle_timestamps CHECK (
        deleted_at IS NULL
        OR disabled_at IS NULL
        OR deleted_at >= disabled_at
    ),
    ADD CONSTRAINT chk_xui_client_provisions_known_traffic CHECK (
        last_known_upload_bytes >= 0
        AND last_known_download_bytes >= 0
        AND last_known_total_bytes >= 0
        AND last_known_total_bytes = last_known_upload_bytes + last_known_download_bytes
    );

CREATE TABLE xui_client_operations (
    id UUID PRIMARY KEY,
    operation_id UUID NOT NULL,
    provision_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_xui_client_operations_provision FOREIGN KEY (provision_id) REFERENCES xui_client_provisions (id),
    CONSTRAINT uq_xui_client_operations_operation_id UNIQUE (operation_id),
    CONSTRAINT chk_xui_client_operations_type CHECK (
        type IN (
            'RENEW_EXPIRY',
            'REPLACE_TRAFFIC_LIMIT',
            'ADD_TRAFFIC',
            'ENABLE',
            'DISABLE',
            'CHANGE_IP_LIMIT',
            'RESET_TRAFFIC',
            'SYNCHRONIZE'
        )
    ),
    CONSTRAINT chk_xui_client_operations_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED', 'UNKNOWN')
    ),
    CONSTRAINT chk_xui_client_operations_fingerprint CHECK (btrim(request_fingerprint) <> ''),
    CONSTRAINT chk_xui_client_operations_completed_at CHECK (
        (status IN ('SUCCEEDED', 'FAILED') AND completed_at IS NOT NULL)
        OR
        (status NOT IN ('SUCCEEDED', 'FAILED'))
    )
);

CREATE INDEX idx_xui_client_operations_provision_id ON xui_client_operations (provision_id);
CREATE INDEX idx_xui_client_operations_operation_id ON xui_client_operations (operation_id);
CREATE INDEX idx_xui_client_operations_type ON xui_client_operations (type);
CREATE INDEX idx_xui_client_operations_status ON xui_client_operations (status);
CREATE INDEX idx_xui_client_operations_requested_at ON xui_client_operations (requested_at);

CREATE UNIQUE INDEX uq_xui_client_operations_in_progress_provision
    ON xui_client_operations (provision_id)
    WHERE status = 'IN_PROGRESS';
