CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_orders_status CHECK (status IN ('CREATED', 'CANCELLED')),
    CONSTRAINT chk_orders_amount CHECK (amount >= 0),
    CONSTRAINT chk_orders_currency CHECK (btrim(currency) <> '')
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    base_amount BIGINT NOT NULL,
    payable_amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    gateway_transaction_id VARCHAR(128),
    gateway_authority VARCHAR(128),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_payments_method CHECK (method IN ('ZARINPAL', 'CARD_TO_CARD')),
    CONSTRAINT chk_payments_status CHECK (
        status IN (
            'CREATED',
            'WAITING_FOR_PAYMENT',
            'PROCESSING',
            'APPROVED',
            'REJECTED',
            'EXPIRED',
            'FAILED',
            'CANCELLED'
        )
    ),
    CONSTRAINT chk_payments_amounts CHECK (base_amount >= 0 AND payable_amount >= base_amount),
    CONSTRAINT chk_payments_currency CHECK (btrim(currency) <> ''),
    CONSTRAINT chk_payments_approved_at CHECK (
        (status = 'APPROVED' AND approved_at IS NOT NULL AND paid_at IS NOT NULL)
        OR
        (status <> 'APPROVED')
    ),
    CONSTRAINT chk_payments_rejected_at CHECK (
        (status = 'REJECTED' AND rejected_at IS NOT NULL)
        OR
        (status <> 'REJECTED')
    )
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_method ON payments (method);

CREATE UNIQUE INDEX uq_payments_one_approved_per_order
    ON payments (order_id)
    WHERE status = 'APPROVED';

CREATE TABLE payment_operations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    operation_type VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_payment_operations_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT chk_payment_operations_type CHECK (
        operation_type IN (
            'CREATED',
            'WAITING_FOR_PAYMENT',
            'PROCESSING',
            'APPROVED',
            'REJECTED',
            'EXPIRED',
            'FAILED',
            'CANCELLED',
            'INITIALIZATION_REQUESTED',
            'VERIFICATION_REQUESTED'
        )
    )
);

CREATE INDEX idx_payment_operations_payment_id ON payment_operations (payment_id);
CREATE INDEX idx_payment_operations_type ON payment_operations (operation_type);
CREATE INDEX idx_payment_operations_occurred_at ON payment_operations (occurred_at);
