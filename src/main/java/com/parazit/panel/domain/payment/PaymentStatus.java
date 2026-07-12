package com.parazit.panel.domain.payment;

public enum PaymentStatus {
    CREATED,
    WAITING_FOR_PAYMENT,
    RECEIPT_SUBMITTED,
    WAITING_FOR_REVIEW,
    PROCESSING,
    APPROVED,
    REJECTED,
    EXPIRED,
    FAILED,
    CANCELLED,
    UNKNOWN
}
