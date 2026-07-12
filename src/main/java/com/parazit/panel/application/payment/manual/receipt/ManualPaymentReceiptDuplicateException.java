package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptDuplicateException extends RuntimeException {
    public ManualPaymentReceiptDuplicateException() {
        super("This receipt file is already active for another manual payment");
    }
}
