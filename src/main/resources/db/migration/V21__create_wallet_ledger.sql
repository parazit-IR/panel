CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    currency VARCHAR(8) NOT NULL,
    balance BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_wallets_user_id UNIQUE (user_id),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallets_currency CHECK (currency = 'IRT'),
    CONSTRAINT chk_wallets_status CHECK (status IN ('ACTIVE', 'LOCKED', 'CLOSED'))
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_status ON wallets (status);

CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(48) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reference_type VARCHAR(80) NOT NULL,
    reference_id UUID,
    idempotency_key VARCHAR(160) NOT NULL,
    description_code VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_wallet_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_wallet_transactions_idempotency UNIQUE (wallet_id, idempotency_key),
    CONSTRAINT chk_wallet_transactions_type CHECK (
        type IN (
            'ADMIN_ADJUSTMENT_CREDIT',
            'ADMIN_ADJUSTMENT_DEBIT',
            'SYSTEM_CREDIT',
            'SYSTEM_DEBIT',
            'TOP_UP',
            'PURCHASE',
            'REFUND',
            'GIFT_CODE',
            'REFERRAL_REWARD',
            'CASHBACK'
        )
    ),
    CONSTRAINT chk_wallet_transactions_direction CHECK (direction IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_wallet_transactions_status CHECK (status IN ('POSTED')),
    CONSTRAINT chk_wallet_transactions_amounts CHECK (
        amount > 0
        AND balance_before >= 0
        AND balance_after >= 0
    ),
    CONSTRAINT chk_wallet_transactions_currency CHECK (currency = 'IRT'),
    CONSTRAINT chk_wallet_transactions_reference_type CHECK (btrim(reference_type) <> ''),
    CONSTRAINT chk_wallet_transactions_idempotency_key CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT chk_wallet_transactions_equation CHECK (
        (direction = 'CREDIT' AND balance_after = balance_before + amount)
        OR
        (direction = 'DEBIT' AND balance_after = balance_before - amount)
    )
);

CREATE INDEX idx_wallet_transactions_wallet_occurred
    ON wallet_transactions (wallet_id, occurred_at DESC, id DESC);

CREATE INDEX idx_wallet_transactions_user_occurred
    ON wallet_transactions (user_id, occurred_at DESC, id DESC);

CREATE INDEX idx_wallet_transactions_reference
    ON wallet_transactions (reference_type, reference_id);
