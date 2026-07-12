package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptRequestIdConflictException extends RuntimeException {
    public ManualPaymentReceiptRequestIdConflictException() {
        super("receiptRequestId belongs to a different receipt submission");
    }
}
