package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptFileTooLargeException extends RuntimeException {
    public ManualPaymentReceiptFileTooLargeException() {
        super("Receipt file is too large");
    }
}
