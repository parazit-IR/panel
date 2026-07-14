CREATE TABLE discount_codes (
    id UUID PRIMARY KEY,
    code_hash VARCHAR(128) NOT NULL UNIQUE,
    masked_code VARCHAR(32) NOT NULL,
    discount_type VARCHAR(32) NOT NULL,
    fixed_amount BIGINT,
    percentage_basis_points INTEGER,
    currency VARCHAR(8) NOT NULL,
    minimum_order_amount BIGINT NOT NULL DEFAULT 0,
    maximum_discount_amount BIGINT,
    allow_new_subscription BOOLEAN NOT NULL DEFAULT TRUE,
    allow_renewal BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    total_usage_limit INTEGER NOT NULL DEFAULT 0,
    per_user_usage_limit INTEGER NOT NULL DEFAULT 1,
    used_count INTEGER NOT NULL DEFAULT 0,
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_discount_codes_type CHECK (discount_type IN ('FIXED_AMOUNT', 'PERCENTAGE')),
    CONSTRAINT chk_discount_codes_status CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'EXHAUSTED')),
    CONSTRAINT chk_discount_codes_amount CHECK (
        (discount_type = 'FIXED_AMOUNT' AND fixed_amount IS NOT NULL AND fixed_amount > 0 AND percentage_basis_points IS NULL)
        OR
        (discount_type = 'PERCENTAGE' AND percentage_basis_points IS NOT NULL AND percentage_basis_points BETWEEN 1 AND 10000 AND fixed_amount IS NULL)
    ),
    CONSTRAINT chk_discount_codes_limits CHECK (
        minimum_order_amount >= 0
        AND (maximum_discount_amount IS NULL OR maximum_discount_amount > 0)
        AND total_usage_limit >= 0
        AND per_user_usage_limit > 0
        AND (total_usage_limit = 0 OR per_user_usage_limit <= total_usage_limit)
        AND used_count >= 0
        AND (total_usage_limit = 0 OR used_count <= total_usage_limit)
    ),
    CONSTRAINT chk_discount_codes_valid_window CHECK (valid_until > valid_from)
);

CREATE TABLE gift_codes (
    id UUID PRIMARY KEY,
    code_hash VARCHAR(128) NOT NULL UNIQUE,
    masked_code VARCHAR(32) NOT NULL,
    credit_amount BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    total_usage_limit INTEGER NOT NULL DEFAULT 0,
    per_user_usage_limit INTEGER NOT NULL DEFAULT 1,
    used_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_gift_codes_status CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'EXHAUSTED')),
    CONSTRAINT chk_gift_codes_amount CHECK (credit_amount > 0),
    CONSTRAINT chk_gift_codes_limits CHECK (
        total_usage_limit >= 0
        AND per_user_usage_limit > 0
        AND (total_usage_limit = 0 OR per_user_usage_limit <= total_usage_limit)
        AND used_count >= 0
        AND (total_usage_limit = 0 OR used_count <= total_usage_limit)
    ),
    CONSTRAINT chk_gift_codes_valid_window CHECK (valid_until > valid_from)
);

ALTER TABLE orders
    ADD COLUMN discount_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN applied_discount_code_id UUID;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_applied_discount_code
        FOREIGN KEY (applied_discount_code_id) REFERENCES discount_codes (id);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_discount_amount
        CHECK (
            discount_amount >= 0
            AND final_amount >= 0
            AND final_amount = base_amount - discount_amount
            AND ((discount_amount = 0 AND applied_discount_code_id IS NULL) OR (discount_amount > 0 AND applied_discount_code_id IS NOT NULL))
        );

CREATE TABLE promotion_redemptions (
    id UUID PRIMARY KEY,
    code_type VARCHAR(32) NOT NULL,
    discount_code_id UUID,
    gift_code_id UUID,
    user_id UUID NOT NULL,
    order_id UUID,
    wallet_id UUID,
    wallet_transaction_id UUID,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(8),
    original_amount BIGINT,
    discount_amount BIGINT,
    final_amount BIGINT,
    gift_amount BIGINT,
    idempotency_key VARCHAR(160) NOT NULL,
    redeemed_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_promotion_redemptions_discount_code FOREIGN KEY (discount_code_id) REFERENCES discount_codes (id),
    CONSTRAINT fk_promotion_redemptions_gift_code FOREIGN KEY (gift_code_id) REFERENCES gift_codes (id),
    CONSTRAINT fk_promotion_redemptions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_promotion_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_promotion_redemptions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_promotion_redemptions_wallet_transaction FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transactions (id),
    CONSTRAINT chk_promotion_redemptions_type CHECK (code_type IN ('DISCOUNT', 'GIFT')),
    CONSTRAINT chk_promotion_redemptions_status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED', 'APPLIED', 'REJECTED')),
    CONSTRAINT chk_promotion_redemptions_target CHECK (
        (code_type = 'DISCOUNT'
            AND discount_code_id IS NOT NULL
            AND gift_code_id IS NULL
            AND order_id IS NOT NULL
            AND wallet_id IS NULL
            AND wallet_transaction_id IS NULL
            AND original_amount IS NOT NULL
            AND discount_amount IS NOT NULL
            AND final_amount IS NOT NULL
            AND gift_amount IS NULL)
        OR
        (code_type = 'GIFT'
            AND gift_code_id IS NOT NULL
            AND discount_code_id IS NULL
            AND order_id IS NULL
            AND wallet_id IS NOT NULL
            AND gift_amount IS NOT NULL)
    ),
    CONSTRAINT chk_promotion_redemptions_amounts CHECK (
        (original_amount IS NULL OR original_amount >= 0)
        AND (discount_amount IS NULL OR discount_amount >= 0)
        AND (final_amount IS NULL OR final_amount >= 0)
        AND (gift_amount IS NULL OR gift_amount > 0)
        AND (code_type <> 'DISCOUNT' OR final_amount = original_amount - discount_amount)
    )
);

CREATE UNIQUE INDEX uq_promotion_discount_order_code
    ON promotion_redemptions (order_id, discount_code_id)
    WHERE code_type = 'DISCOUNT' AND status IN ('RESERVED', 'CONSUMED');

CREATE UNIQUE INDEX uq_promotion_gift_user_code
    ON promotion_redemptions (user_id, gift_code_id)
    WHERE code_type = 'GIFT';

CREATE UNIQUE INDEX uq_promotion_wallet_transaction
    ON promotion_redemptions (wallet_transaction_id)
    WHERE wallet_transaction_id IS NOT NULL;

CREATE INDEX idx_promotion_redemptions_user_discount
    ON promotion_redemptions (user_id, discount_code_id);

CREATE INDEX idx_promotion_redemptions_user_gift
    ON promotion_redemptions (user_id, gift_code_id);

CREATE INDEX idx_promotion_redemptions_order
    ON promotion_redemptions (order_id);

CREATE INDEX idx_promotion_redemptions_status_created
    ON promotion_redemptions (status, created_at);

ALTER TABLE wallet_transactions
    DROP CONSTRAINT chk_wallet_transactions_type;

ALTER TABLE wallet_transactions
    ADD CONSTRAINT chk_wallet_transactions_type
        CHECK (type IN ('ADMIN_ADJUSTMENT_CREDIT', 'ADMIN_ADJUSTMENT_DEBIT', 'SYSTEM_CREDIT', 'SYSTEM_DEBIT', 'TOP_UP', 'PURCHASE', 'REFUND', 'GIFT_CODE', 'REFERRAL_REWARD', 'CASHBACK'));

ALTER TABLE telegram_sensitive_actions
    DROP CONSTRAINT chk_telegram_sensitive_actions_type;

ALTER TABLE telegram_sensitive_actions
    ADD CONSTRAINT chk_telegram_sensitive_actions_type
        CHECK (type IN ('ROTATE_SUBSCRIPTION_TOKEN', 'CONFIRM_WALLET_ORDER_PAYMENT'));

ALTER TABLE telegram_sensitive_actions
    DROP CONSTRAINT chk_telegram_sensitive_actions_target;

ALTER TABLE telegram_sensitive_actions
    ADD CONSTRAINT chk_telegram_sensitive_actions_target
        CHECK (
            (type = 'ROTATE_SUBSCRIPTION_TOKEN' AND subscription_id IS NOT NULL AND resource_id IS NULL)
            OR
            (type = 'CONFIRM_WALLET_ORDER_PAYMENT' AND resource_id IS NOT NULL)
        );
