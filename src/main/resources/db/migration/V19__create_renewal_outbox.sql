ALTER TABLE orders
    DROP CONSTRAINT chk_orders_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status CHECK (
        status IN (
            'CREATED',
            'PAYMENT_PENDING',
            'PAID',
            'RENEWAL_PENDING',
            'RENEWAL_REVIEW_REQUIRED',
            'PROVISIONING',
            'COMPLETED',
            'PROVISIONING_FAILED',
            'CANCELLED',
            'EXPIRED'
        )
    );

DROP INDEX IF EXISTS uq_orders_one_active_renewal_per_target;

CREATE UNIQUE INDEX uq_orders_one_active_renewal_per_target
    ON orders (target_subscription_id)
    WHERE type = 'RENEWAL'
      AND status IN (
          'CREATED',
          'PAYMENT_PENDING',
          'PAID',
          'RENEWAL_PENDING',
          'RENEWAL_REVIEW_REQUIRED',
          'PROVISIONING',
          'PROVISIONING_FAILED'
      )
      AND target_subscription_id IS NOT NULL;

CREATE TABLE renewal_outbox (
    id UUID PRIMARY KEY,
    renewal_order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    target_subscription_id UUID NOT NULL,
    target_provision_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload_version VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(128),
    last_error_code VARCHAR(64),
    processed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_renewal_outbox_order FOREIGN KEY (renewal_order_id) REFERENCES orders (id),
    CONSTRAINT fk_renewal_outbox_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_renewal_outbox_subscription FOREIGN KEY (target_subscription_id) REFERENCES subscriptions (id),
    CONSTRAINT fk_renewal_outbox_provision FOREIGN KEY (target_provision_id) REFERENCES xui_client_provisions (id),
    CONSTRAINT uq_renewal_outbox_order_event UNIQUE (renewal_order_id, event_type),
    CONSTRAINT chk_renewal_outbox_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD')
    ),
    CONSTRAINT chk_renewal_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_renewal_outbox_event_type CHECK (event_type <> ''),
    CONSTRAINT chk_renewal_outbox_payload_version CHECK (payload_version <> '')
);

CREATE INDEX idx_renewal_outbox_status_available
    ON renewal_outbox (status, available_at);

CREATE INDEX idx_renewal_outbox_payment_id
    ON renewal_outbox (payment_id);

CREATE INDEX idx_renewal_outbox_target_subscription
    ON renewal_outbox (target_subscription_id);
