package com.parazit.panel.domain.payment;

public enum PaymentStatus {
    CREATED,
    WAITING_FOR_PAYMENT,
    PROCESSING,
    APPROVED,
    REJECTED,
    EXPIRED,
    FAILED,
    CANCELLED
}
