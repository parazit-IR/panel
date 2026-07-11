ALTER TABLE xui_client_provisions
    ADD COLUMN disabled_at TIMESTAMPTZ,
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE xui_client_provisions
    DROP CONSTRAINT chk_xui_client_provisions_status,
    DROP CONSTRAINT chk_xui_client_provisions_active_provisioned;

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
    );
