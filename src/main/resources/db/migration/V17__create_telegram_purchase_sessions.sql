CREATE TABLE telegram_purchase_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    telegram_user_id BIGINT NOT NULL,
    plan_selection_id UUID NOT NULL,
    order_id UUID,
    payment_id UUID,
    status VARCHAR(40) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_telegram_purchase_sessions_user_id ON telegram_purchase_sessions(user_id);
CREATE INDEX idx_telegram_purchase_sessions_telegram_user ON telegram_purchase_sessions(telegram_user_id);
CREATE INDEX idx_telegram_purchase_sessions_selection ON telegram_purchase_sessions(plan_selection_id);
CREATE INDEX idx_telegram_purchase_sessions_order ON telegram_purchase_sessions(order_id);
CREATE INDEX idx_telegram_purchase_sessions_payment ON telegram_purchase_sessions(payment_id);
CREATE INDEX idx_telegram_purchase_sessions_status ON telegram_purchase_sessions(status);
CREATE INDEX idx_telegram_purchase_sessions_expires_at ON telegram_purchase_sessions(expires_at);

CREATE UNIQUE INDEX uq_telegram_purchase_sessions_active_user
    ON telegram_purchase_sessions(user_id)
    WHERE status IN ('PLAN_SELECTED', 'PRE_INVOICE_SHOWN', 'ORDER_CREATED', 'PAYMENT_METHODS_SHOWN', 'PAYMENT_CREATED');

CREATE UNIQUE INDEX uq_orders_plan_selection_id
    ON orders(plan_selection_id)
    WHERE plan_selection_id IS NOT NULL;
