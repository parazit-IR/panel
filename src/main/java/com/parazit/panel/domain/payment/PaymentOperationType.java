package com.parazit.panel.domain.payment;

public enum PaymentOperationType {
    CREATED,
    WAITING_FOR_PAYMENT,
    PROCESSING,
    APPROVED,
    REJECTED,
    EXPIRED,
    FAILED,
    CANCELLED,
    INITIALIZATION_REQUESTED,
    VERIFICATION_REQUESTED
}
