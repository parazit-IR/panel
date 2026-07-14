package com.parazit.panel.domain.order;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    RENEWAL_PENDING,
    RENEWAL_REVIEW_REQUIRED,
    PROVISIONING,
    COMPLETED,
    PROVISIONING_FAILED,
    CANCELLED,
    EXPIRED
}
