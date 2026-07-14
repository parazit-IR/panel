ALTER TABLE payments
    ADD COLUMN wallet_transaction_id UUID;

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_wallet_transaction
        FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transactions (id);

CREATE UNIQUE INDEX uq_payments_wallet_transaction_id
    ON payments (wallet_transaction_id)
    WHERE wallet_transaction_id IS NOT NULL;

CREATE INDEX idx_payments_order_method_status
    ON payments (order_id, method, status)
    WHERE order_id IS NOT NULL;

ALTER TABLE payments
    DROP CONSTRAINT chk_payments_method;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_method
        CHECK (method IN ('ZARINPAL', 'CARD_TO_CARD', 'WALLET'));

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_wallet_method
        CHECK (
            method <> 'WALLET'
            OR (
                target_type = 'ORDER'
                AND order_id IS NOT NULL
                AND wallet_top_up_request_id IS NULL
                AND gateway_transaction_id IS NULL
                AND gateway_authority IS NULL
            )
        );

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_wallet_approved_transaction
        CHECK (
            method <> 'WALLET'
            OR status <> 'APPROVED'
            OR wallet_transaction_id IS NOT NULL
        );

ALTER TABLE telegram_sensitive_actions
    ALTER COLUMN subscription_id DROP NOT NULL,
    ADD COLUMN resource_id UUID;

CREATE INDEX idx_telegram_sensitive_actions_resource
    ON telegram_sensitive_actions (resource_id)
    WHERE resource_id IS NOT NULL;

ALTER TABLE telegram_sensitive_actions
    DROP CONSTRAINT chk_telegram_sensitive_actions_type;

ALTER TABLE telegram_sensitive_actions
    ADD CONSTRAINT chk_telegram_sensitive_actions_type
        CHECK (type IN ('ROTATE_SUBSCRIPTION_TOKEN', 'CONFIRM_WALLET_ORDER_PAYMENT'));

ALTER TABLE telegram_sensitive_actions
    ADD CONSTRAINT chk_telegram_sensitive_actions_target
        CHECK (
            (type = 'ROTATE_SUBSCRIPTION_TOKEN' AND subscription_id IS NOT NULL AND resource_id IS NULL)
            OR
            (type = 'CONFIRM_WALLET_ORDER_PAYMENT' AND resource_id IS NOT NULL)
        );
