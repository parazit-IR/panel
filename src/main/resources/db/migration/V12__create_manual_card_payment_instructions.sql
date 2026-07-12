CREATE TABLE manual_card_payment_instructions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    instruction_request_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    base_amount BIGINT NOT NULL,
    unique_suffix_amount BIGINT NOT NULL,
    payable_amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    destination_id VARCHAR(64) NOT NULL,
    bank_name_snapshot VARCHAR(128) NOT NULL,
    card_holder_name_snapshot VARCHAR(128) NOT NULL,
    card_number_masked_snapshot VARCHAR(32) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_claimed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_manual_card_instructions_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT uq_manual_card_instructions_request_id UNIQUE (instruction_request_id),
    CONSTRAINT chk_manual_card_instruction_status CHECK (
        status IN (
            'CREATED',
            'ACTIVE',
            'RECEIPT_PENDING',
            'EXPIRED',
            'CANCELLED',
            'COMPLETED'
        )
    ),
    CONSTRAINT chk_manual_card_instruction_amounts CHECK (
        base_amount > 0
        AND unique_suffix_amount > 0
        AND payable_amount = base_amount + unique_suffix_amount
    ),
    CONSTRAINT chk_manual_card_instruction_currency CHECK (btrim(currency) <> ''),
    CONSTRAINT chk_manual_card_instruction_expiry CHECK (expires_at > issued_at),
    CONSTRAINT chk_manual_card_instruction_expired_at CHECK (
        (status = 'EXPIRED' AND expired_at IS NOT NULL)
        OR
        (status <> 'EXPIRED')
    ),
    CONSTRAINT chk_manual_card_instruction_cancelled_at CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
        OR
        (status <> 'CANCELLED')
    )
);

CREATE INDEX idx_manual_card_instructions_payment_id ON manual_card_payment_instructions (payment_id);
CREATE INDEX idx_manual_card_instructions_request_id ON manual_card_payment_instructions (instruction_request_id);
CREATE INDEX idx_manual_card_instructions_status ON manual_card_payment_instructions (status);
CREATE INDEX idx_manual_card_instructions_expires_at ON manual_card_payment_instructions (expires_at);
CREATE INDEX idx_manual_card_instructions_amount ON manual_card_payment_instructions (currency, payable_amount);

CREATE UNIQUE INDEX uq_manual_card_active_payment
    ON manual_card_payment_instructions (payment_id)
    WHERE status IN ('CREATED', 'ACTIVE', 'RECEIPT_PENDING');

CREATE UNIQUE INDEX uq_manual_card_active_amount
    ON manual_card_payment_instructions (currency, payable_amount)
    WHERE status IN ('CREATED', 'ACTIVE', 'RECEIPT_PENDING');
