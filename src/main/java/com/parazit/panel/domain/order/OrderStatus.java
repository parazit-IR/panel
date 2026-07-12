package com.parazit.panel.domain.order;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PROVISIONING,
    COMPLETED,
    PROVISIONING_FAILED,
    CANCELLED,
    EXPIRED
}
