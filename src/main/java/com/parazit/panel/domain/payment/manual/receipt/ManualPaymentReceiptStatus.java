package com.parazit.panel.domain.payment.manual.receipt;

public enum ManualPaymentReceiptStatus {
    UPLOADING,
    SUBMITTED,
    QUEUED_FOR_REVIEW,
    WITHDRAWN,
    APPROVED,
    REJECTED,
    INVALID_FILE
}
