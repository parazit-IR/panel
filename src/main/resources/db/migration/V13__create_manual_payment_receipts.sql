ALTER TABLE payments
    DROP CONSTRAINT chk_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status CHECK (
        status IN (
            'CREATED',
            'WAITING_FOR_PAYMENT',
            'RECEIPT_SUBMITTED',
            'WAITING_FOR_REVIEW',
            'PROCESSING',
            'APPROVED',
            'REJECTED',
            'EXPIRED',
            'FAILED',
            'CANCELLED',
            'UNKNOWN'
        )
    );

CREATE TABLE manual_payment_receipts (
    id UUID PRIMARY KEY,
    receipt_request_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    instruction_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    storage_provider VARCHAR(32),
    storage_key VARCHAR(512),
    original_filename VARCHAR(255) NOT NULL,
    sanitized_filename VARCHAR(255),
    detected_content_type VARCHAR(128),
    file_size_bytes BIGINT,
    file_sha256 VARCHAR(64),
    claimed_tracking_number VARCHAR(128),
    claimed_sender_card_last_four VARCHAR(4),
    claimed_paid_at TIMESTAMPTZ,
    claimed_amount BIGINT NOT NULL,
    user_note VARCHAR(1000),
    duplicate_hash_detected BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMPTZ NOT NULL,
    review_queued_at TIMESTAMPTZ,
    withdrawn_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_manual_receipts_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_manual_receipts_instruction FOREIGN KEY (instruction_id) REFERENCES manual_card_payment_instructions (id),
    CONSTRAINT fk_manual_receipts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_manual_receipts_request_id UNIQUE (receipt_request_id),
    CONSTRAINT chk_manual_receipts_status CHECK (
        status IN (
            'UPLOADING',
            'SUBMITTED',
            'QUEUED_FOR_REVIEW',
            'WITHDRAWN',
            'APPROVED',
            'REJECTED',
            'INVALID_FILE'
        )
    ),
    CONSTRAINT chk_manual_receipts_claimed_amount CHECK (claimed_amount > 0),
    CONSTRAINT chk_manual_receipts_file_size CHECK (file_size_bytes IS NULL OR file_size_bytes > 0),
    CONSTRAINT chk_manual_receipts_sha256 CHECK (file_sha256 IS NULL OR file_sha256 ~ '^[a-f0-9]{64}$'),
    CONSTRAINT chk_manual_receipts_last_four CHECK (
        claimed_sender_card_last_four IS NULL
        OR claimed_sender_card_last_four ~ '^[0-9]{4}$'
    ),
    CONSTRAINT chk_manual_receipts_stored_metadata CHECK (
        (
            status IN ('SUBMITTED', 'QUEUED_FOR_REVIEW', 'APPROVED', 'REJECTED')
            AND storage_provider IS NOT NULL
            AND storage_key IS NOT NULL
            AND sanitized_filename IS NOT NULL
            AND detected_content_type IS NOT NULL
            AND file_size_bytes IS NOT NULL
            AND file_sha256 IS NOT NULL
        )
        OR status IN ('UPLOADING', 'WITHDRAWN', 'INVALID_FILE')
    ),
    CONSTRAINT chk_manual_receipts_review_queued CHECK (
        (status = 'QUEUED_FOR_REVIEW' AND review_queued_at IS NOT NULL)
        OR status <> 'QUEUED_FOR_REVIEW'
    ),
    CONSTRAINT chk_manual_receipts_withdrawn CHECK (
        (status = 'WITHDRAWN' AND withdrawn_at IS NOT NULL)
        OR status <> 'WITHDRAWN'
    )
);

CREATE INDEX idx_manual_receipts_payment_id ON manual_payment_receipts (payment_id);
CREATE INDEX idx_manual_receipts_instruction_id ON manual_payment_receipts (instruction_id);
CREATE INDEX idx_manual_receipts_user_id ON manual_payment_receipts (user_id);
CREATE INDEX idx_manual_receipts_status ON manual_payment_receipts (status);
CREATE INDEX idx_manual_receipts_submitted_at ON manual_payment_receipts (submitted_at);
CREATE INDEX idx_manual_receipts_review_queued_at ON manual_payment_receipts (review_queued_at);
CREATE INDEX idx_manual_receipts_file_sha256 ON manual_payment_receipts (file_sha256);

CREATE UNIQUE INDEX uq_manual_receipts_active_instruction
    ON manual_payment_receipts (instruction_id)
    WHERE status IN ('UPLOADING', 'SUBMITTED', 'QUEUED_FOR_REVIEW');
