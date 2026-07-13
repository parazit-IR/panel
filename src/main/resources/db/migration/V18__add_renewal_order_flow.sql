ALTER TABLE plans
    ADD COLUMN renewal_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_plans_renewal_enabled
    ON plans (renewal_enabled);

ALTER TABLE plan_selections
    ADD COLUMN selection_type VARCHAR(40) NOT NULL DEFAULT 'NEW_SUBSCRIPTION',
    ADD COLUMN target_subscription_id UUID;

ALTER TABLE plan_selections
    ADD CONSTRAINT fk_plan_selections_target_subscription
        FOREIGN KEY (target_subscription_id) REFERENCES subscriptions (id),
    ADD CONSTRAINT chk_plan_selections_selection_type
        CHECK (selection_type IN ('NEW_SUBSCRIPTION', 'RENEWAL')),
    ADD CONSTRAINT chk_plan_selections_renewal_target
        CHECK (
            (selection_type = 'NEW_SUBSCRIPTION' AND target_subscription_id IS NULL)
            OR
            (selection_type = 'RENEWAL' AND target_subscription_id IS NOT NULL)
        );

CREATE INDEX idx_plan_selections_type_user_status
    ON plan_selections (selection_type, user_id, status);

CREATE INDEX idx_plan_selections_target_type_status
    ON plan_selections (target_subscription_id, selection_type, status)
    WHERE target_subscription_id IS NOT NULL;

ALTER TABLE telegram_purchase_sessions
    ADD COLUMN flow_type VARCHAR(40) NOT NULL DEFAULT 'NEW_SUBSCRIPTION',
    ADD COLUMN target_subscription_id UUID;

ALTER TABLE telegram_purchase_sessions
    ADD CONSTRAINT fk_telegram_purchase_sessions_target_subscription
        FOREIGN KEY (target_subscription_id) REFERENCES subscriptions (id),
    ADD CONSTRAINT chk_telegram_purchase_sessions_flow_type
        CHECK (flow_type IN ('NEW_SUBSCRIPTION', 'RENEWAL')),
    ADD CONSTRAINT chk_telegram_purchase_sessions_renewal_target
        CHECK (
            (flow_type = 'NEW_SUBSCRIPTION' AND target_subscription_id IS NULL)
            OR
            (flow_type = 'RENEWAL' AND target_subscription_id IS NOT NULL)
        );

CREATE INDEX idx_telegram_purchase_sessions_user_flow_status
    ON telegram_purchase_sessions (user_id, flow_type, status);

CREATE INDEX idx_telegram_purchase_sessions_target_flow_status
    ON telegram_purchase_sessions (target_subscription_id, flow_type, status)
    WHERE target_subscription_id IS NOT NULL;

ALTER TABLE orders
    ADD COLUMN target_subscription_id UUID,
    ADD COLUMN renewal_snapshot JSONB;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_target_subscription
        FOREIGN KEY (target_subscription_id) REFERENCES subscriptions (id),
    ADD CONSTRAINT chk_orders_renewal_target_snapshot
        CHECK (
            (
                type = 'NEW_SUBSCRIPTION'
                AND target_subscription_id IS NULL
                AND renewal_snapshot IS NULL
            )
            OR
            (
                type = 'RENEWAL'
                AND target_subscription_id IS NOT NULL
                AND renewal_snapshot IS NOT NULL
            )
            OR
            (
                type = 'TRAFFIC_ADDON'
                AND renewal_snapshot IS NULL
            )
        );

CREATE INDEX idx_orders_target_subscription_type_status
    ON orders (target_subscription_id, type, status)
    WHERE target_subscription_id IS NOT NULL;

CREATE UNIQUE INDEX uq_orders_one_active_renewal_per_target
    ON orders (target_subscription_id)
    WHERE type = 'RENEWAL'
      AND status IN ('CREATED', 'PAYMENT_PENDING', 'PAID', 'PROVISIONING', 'PROVISIONING_FAILED')
      AND target_subscription_id IS NOT NULL;
