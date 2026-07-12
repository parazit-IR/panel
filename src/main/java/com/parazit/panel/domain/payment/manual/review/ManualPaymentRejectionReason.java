package com.parazit.panel.domain.payment.manual.review;

public enum ManualPaymentRejectionReason {
    AMOUNT_MISMATCH,
    INVALID_RECEIPT,
    DUPLICATE_RECEIPT,
    UNREADABLE_RECEIPT,
    WRONG_DESTINATION,
    TRANSACTION_NOT_FOUND,
    EXPIRED_INSTRUCTION,
    OTHER
}
