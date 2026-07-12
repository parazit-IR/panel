ALTER TABLE payments
    DROP CONSTRAINT chk_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status CHECK (
        status IN (
            'CREATED',
            'WAITING_FOR_PAYMENT',
            'PROCESSING',
            'APPROVED',
            'REJECTED',
            'EXPIRED',
            'FAILED',
            'CANCELLED',
            'UNKNOWN'
        )
    );

CREATE TABLE zarinpal_payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    request_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    gateway_amount BIGINT NOT NULL,
    authority VARCHAR(64),
    reference_id VARCHAR(128),
    gateway_code VARCHAR(32),
    card_hash VARCHAR(128),
    card_pan_masked VARCHAR(32),
    requested_at TIMESTAMPTZ,
    redirected_at TIMESTAMPTZ,
    callback_received_at TIMESTAMPTZ,
    verification_started_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_code VARCHAR(64),
    failure_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_zarinpal_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT uq_zarinpal_attempts_request_id UNIQUE (request_id),
    CONSTRAINT chk_zarinpal_attempts_status CHECK (
        status IN (
            'CREATED',
            'REQUESTING',
            'REDIRECT_READY',
            'CALLBACK_RECEIVED',
            'VERIFYING',
            'VERIFIED',
            'CANCELLED',
            'FAILED',
            'UNKNOWN'
        )
    ),
    CONSTRAINT chk_zarinpal_attempts_amount CHECK (gateway_amount > 0),
    CONSTRAINT chk_zarinpal_attempts_redirect_ready CHECK (
        (status = 'REDIRECT_READY' AND authority IS NOT NULL AND redirected_at IS NOT NULL)
        OR
        (status <> 'REDIRECT_READY')
    ),
    CONSTRAINT chk_zarinpal_attempts_verified CHECK (
        (status = 'VERIFIED' AND verified_at IS NOT NULL AND reference_id IS NOT NULL)
        OR
        (status <> 'VERIFIED')
    )
);

CREATE UNIQUE INDEX uq_zarinpal_attempts_authority
    ON zarinpal_payment_attempts (authority)
    WHERE authority IS NOT NULL;

CREATE UNIQUE INDEX uq_zarinpal_attempts_reference_id
    ON zarinpal_payment_attempts (reference_id)
    WHERE reference_id IS NOT NULL;

CREATE INDEX idx_zarinpal_attempts_payment_id ON zarinpal_payment_attempts (payment_id);
CREATE INDEX idx_zarinpal_attempts_request_id ON zarinpal_payment_attempts (request_id);
CREATE INDEX idx_zarinpal_attempts_authority ON zarinpal_payment_attempts (authority);
CREATE INDEX idx_zarinpal_attempts_reference_id ON zarinpal_payment_attempts (reference_id);
CREATE INDEX idx_zarinpal_attempts_status ON zarinpal_payment_attempts (status);
CREATE INDEX idx_zarinpal_attempts_created_at ON zarinpal_payment_attempts (created_at);
