CREATE TABLE wallet_top_up_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    wallet_id UUID NOT NULL,
    requested_amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(40) NOT NULL,
    payment_id UUID,
    idempotency_key VARCHAR(160) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    credited_at TIMESTAMPTZ,
    failed_reason VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_wallet_top_up_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_wallet_top_up_requests_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_wallet_top_up_requests_amount CHECK (requested_amount > 0),
    CONSTRAINT chk_wallet_top_up_requests_currency CHECK (currency = 'IRT'),
    CONSTRAINT chk_wallet_top_up_requests_status CHECK (
        status IN (
            'CREATED',
            'AWAITING_PAYMENT_METHOD',
            'PENDING_PAYMENT',
            'PAYMENT_APPROVED',
            'CREDITED',
            'CANCELLED',
            'EXPIRED',
            'FAILED'
        )
    ),
    CONSTRAINT chk_wallet_top_up_requests_idempotency CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT chk_wallet_top_up_requests_credited CHECK (
        (status = 'CREDITED' AND credited_at IS NOT NULL)
        OR status <> 'CREDITED'
    )
);

CREATE UNIQUE INDEX uq_wallet_top_up_requests_idempotency
    ON wallet_top_up_requests (user_id, idempotency_key);

CREATE UNIQUE INDEX uq_wallet_top_up_requests_payment_id
    ON wallet_top_up_requests (payment_id)
    WHERE payment_id IS NOT NULL;

CREATE INDEX idx_wallet_top_up_requests_user_status_created
    ON wallet_top_up_requests (user_id, status, created_at DESC);

CREATE INDEX idx_wallet_top_up_requests_wallet_status
    ON wallet_top_up_requests (wallet_id, status);

ALTER TABLE payments
    ADD COLUMN target_type VARCHAR(40) NOT NULL DEFAULT 'ORDER',
    ADD COLUMN wallet_top_up_request_id UUID;

ALTER TABLE payments
    ALTER COLUMN order_id DROP NOT NULL;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_wallet_top_up_request
        FOREIGN KEY (wallet_top_up_request_id) REFERENCES wallet_top_up_requests (id),
    ADD CONSTRAINT chk_payments_target_type
        CHECK (target_type IN ('ORDER', 'WALLET_TOP_UP')),
    ADD CONSTRAINT chk_payments_target_consistency
        CHECK (
            (target_type = 'ORDER' AND order_id IS NOT NULL AND wallet_top_up_request_id IS NULL)
            OR
            (target_type = 'WALLET_TOP_UP' AND order_id IS NULL AND wallet_top_up_request_id IS NOT NULL)
        );

CREATE INDEX idx_payments_wallet_top_up_request_id
    ON payments (wallet_top_up_request_id)
    WHERE wallet_top_up_request_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payments_one_approved_per_wallet_top_up
    ON payments (wallet_top_up_request_id)
    WHERE target_type = 'WALLET_TOP_UP'
      AND status = 'APPROVED'
      AND wallet_top_up_request_id IS NOT NULL;

ALTER TABLE wallet_top_up_requests
    ADD CONSTRAINT fk_wallet_top_up_requests_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id);

ALTER TABLE manual_payment_reviews
    ALTER COLUMN order_id DROP NOT NULL;

ALTER TABLE manual_payment_reviews
    DROP CONSTRAINT fk_manual_reviews_order;

ALTER TABLE manual_payment_reviews
    ADD CONSTRAINT fk_manual_reviews_order
        FOREIGN KEY (order_id) REFERENCES orders (id);
