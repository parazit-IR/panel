ALTER TABLE renewal_outbox
    ADD COLUMN execution_step VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN target_payload_version VARCHAR(64),
    ADD COLUMN target_payload JSONB;

ALTER TABLE renewal_outbox
    ADD CONSTRAINT chk_renewal_outbox_execution_step CHECK (
        execution_step IN (
            'NOT_STARTED',
            'TARGET_CALCULATED',
            'CLIENT_UPDATED',
            'TRAFFIC_RESET',
            'REMOTE_VERIFIED',
            'LOCAL_STATE_UPDATED',
            'COMPLETED'
        )
    ),
    ADD CONSTRAINT chk_renewal_outbox_target_payload_version CHECK (
        target_payload_version IS NULL OR btrim(target_payload_version) <> ''
    );

CREATE INDEX idx_renewal_outbox_locked_at
    ON renewal_outbox (locked_at);

CREATE TABLE subscription_renewal_history (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    renewal_order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    previous_expiry_at TIMESTAMPTZ,
    new_expiry_at TIMESTAMPTZ NOT NULL,
    previous_traffic_limit_bytes BIGINT,
    new_traffic_limit_bytes BIGINT NOT NULL,
    previous_used_traffic_bytes BIGINT,
    new_used_traffic_bytes BIGINT NOT NULL,
    traffic_policy VARCHAR(40) NOT NULL,
    expiry_policy VARCHAR(60) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_request_id VARCHAR(128),
    provider_status_code VARCHAR(64),
    applied_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_subscription_renewal_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id),
    CONSTRAINT fk_subscription_renewal_history_order FOREIGN KEY (renewal_order_id) REFERENCES orders (id),
    CONSTRAINT fk_subscription_renewal_history_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_subscription_renewal_history_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT uq_subscription_renewal_history_order UNIQUE (renewal_order_id),
    CONSTRAINT chk_subscription_renewal_history_status CHECK (status IN ('APPLIED')),
    CONSTRAINT chk_subscription_renewal_history_traffic CHECK (
        (previous_traffic_limit_bytes IS NULL OR previous_traffic_limit_bytes >= 0)
        AND new_traffic_limit_bytes >= 0
        AND (previous_used_traffic_bytes IS NULL OR previous_used_traffic_bytes >= 0)
        AND new_used_traffic_bytes >= 0
    )
);

CREATE INDEX idx_subscription_renewal_history_subscription_applied
    ON subscription_renewal_history (subscription_id, applied_at DESC);
