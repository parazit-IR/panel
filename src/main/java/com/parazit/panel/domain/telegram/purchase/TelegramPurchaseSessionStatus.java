package com.parazit.panel.domain.telegram.purchase;

public enum TelegramPurchaseSessionStatus {
    PLAN_SELECTED,
    PRE_INVOICE_SHOWN,
    ORDER_CREATED,
    PAYMENT_METHODS_SHOWN,
    PAYMENT_CREATED,
    COMPLETED,
    EXPIRED,
    CANCELLED
}
