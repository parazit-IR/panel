ALTER TABLE orders
    ADD COLUMN plan_id UUID,
    ADD COLUMN plan_selection_id UUID,
    ADD COLUMN type VARCHAR(40) NOT NULL DEFAULT 'NEW_SUBSCRIPTION',
    ADD COLUMN base_amount BIGINT,
    ADD COLUMN final_amount BIGINT,
    ADD COLUMN paid_at TIMESTAMPTZ,
    ADD COLUMN cancelled_at TIMESTAMPTZ,
    ADD COLUMN completed_at TIMESTAMPTZ,
    ADD COLUMN failure_code VARCHAR(64),
    ADD COLUMN failure_message VARCHAR(500);

UPDATE orders
SET base_amount = amount,
    final_amount = amount
WHERE base_amount IS NULL
   OR final_amount IS NULL;

ALTER TABLE orders
    ALTER COLUMN base_amount SET NOT NULL,
    ALTER COLUMN final_amount SET NOT NULL,
    ADD CONSTRAINT fk_orders_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    ADD CONSTRAINT fk_orders_plan_selection FOREIGN KEY (plan_selection_id) REFERENCES plan_selections (id),
    ADD CONSTRAINT chk_orders_type CHECK (type IN ('NEW_SUBSCRIPTION', 'RENEWAL', 'TRAFFIC_ADDON')),
    ADD CONSTRAINT chk_orders_amount_snapshots CHECK (base_amount >= 0 AND final_amount >= base_amount),
    ADD CONSTRAINT chk_orders_plan_linkage CHECK (
        (plan_id IS NULL AND plan_selection_id IS NULL)
        OR
        (plan_id IS NOT NULL AND plan_selection_id IS NOT NULL)
    );

ALTER TABLE orders
    DROP CONSTRAINT chk_orders_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status CHECK (
        status IN (
            'CREATED',
            'PAYMENT_PENDING',
            'PAID',
            'PROVISIONING',
            'COMPLETED',
            'PROVISIONING_FAILED',
            'CANCELLED',
            'EXPIRED'
        )
    );

CREATE INDEX idx_orders_plan_selection_id ON orders (plan_selection_id);
CREATE INDEX idx_orders_plan_id ON orders (plan_id);

CREATE TABLE manual_payment_reviews (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    order_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    reviewer_id VARCHAR(128),
    claimed_at TIMESTAMPTZ,
    review_started_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    decision_reason VARCHAR(64),
    operator_note VARCHAR(1000),
    expected_amount BIGINT NOT NULL,
    claimed_amount BIGINT NOT NULL,
    amount_matched BOOLEAN NOT NULL,
    duplicate_hash_detected BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_manual_reviews_receipt_id UNIQUE (receipt_id),
    CONSTRAINT fk_manual_reviews_receipt FOREIGN KEY (receipt_id) REFERENCES manual_payment_receipts (id),
    CONSTRAINT fk_manual_reviews_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_manual_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_manual_reviews_status CHECK (
        status IN ('PENDING', 'CLAIMED', 'APPROVED', 'REJECTED', 'RELEASED')
    ),
    CONSTRAINT chk_manual_reviews_amounts CHECK (expected_amount > 0 AND claimed_amount > 0),
    CONSTRAINT chk_manual_reviews_claimed CHECK (
        (status = 'CLAIMED' AND reviewer_id IS NOT NULL AND claimed_at IS NOT NULL)
        OR status <> 'CLAIMED'
    ),
    CONSTRAINT chk_manual_reviews_decided CHECK (
        (status IN ('APPROVED', 'REJECTED') AND reviewer_id IS NOT NULL AND decided_at IS NOT NULL)
        OR status NOT IN ('APPROVED', 'REJECTED')
    ),
    CONSTRAINT chk_manual_reviews_rejection_reason CHECK (
        (status = 'REJECTED' AND decision_reason IS NOT NULL)
        OR status <> 'REJECTED'
    )
);

CREATE INDEX idx_manual_reviews_payment_id ON manual_payment_reviews (payment_id);
CREATE INDEX idx_manual_reviews_order_id ON manual_payment_reviews (order_id);
CREATE INDEX idx_manual_reviews_status ON manual_payment_reviews (status);
CREATE INDEX idx_manual_reviews_reviewer_id ON manual_payment_reviews (reviewer_id);
CREATE INDEX idx_manual_reviews_claimed_at ON manual_payment_reviews (claimed_at);
CREATE INDEX idx_manual_reviews_decided_at ON manual_payment_reviews (decided_at);

CREATE TABLE provisioning_outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    plan_selection_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload_version VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    attempt_count INTEGER NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    processing_started_at TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    last_failed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_provisioning_outbox_event_id UNIQUE (event_id),
    CONSTRAINT uq_provisioning_outbox_order_type UNIQUE (order_id, type),
    CONSTRAINT fk_provisioning_outbox_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_provisioning_outbox_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_provisioning_outbox_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_provisioning_outbox_plan FOREIGN KEY (plan_id) REFERENCES plans (id),
    CONSTRAINT fk_provisioning_outbox_plan_selection FOREIGN KEY (plan_selection_id) REFERENCES plan_selections (id),
    CONSTRAINT chk_provisioning_outbox_type CHECK (
        type IN ('CREATE_VPN_CLIENT', 'RENEW_VPN_CLIENT', 'ADD_VPN_TRAFFIC')
    ),
    CONSTRAINT chk_provisioning_outbox_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'UNKNOWN', 'DEAD')
    ),
    CONSTRAINT chk_provisioning_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT chk_provisioning_outbox_processed CHECK (
        (status = 'PROCESSED' AND processed_at IS NOT NULL)
        OR status <> 'PROCESSED'
    ),
    CONSTRAINT chk_provisioning_outbox_processing CHECK (
        (status = 'PROCESSING' AND processing_started_at IS NOT NULL)
        OR status <> 'PROCESSING'
    )
);

CREATE INDEX idx_provisioning_outbox_status_available ON provisioning_outbox (status, available_at);
CREATE INDEX idx_provisioning_outbox_order_id ON provisioning_outbox (order_id);
CREATE INDEX idx_provisioning_outbox_payment_id ON provisioning_outbox (payment_id);
CREATE INDEX idx_provisioning_outbox_user_id ON provisioning_outbox (user_id);
CREATE INDEX idx_provisioning_outbox_plan_selection_id ON provisioning_outbox (plan_selection_id);
