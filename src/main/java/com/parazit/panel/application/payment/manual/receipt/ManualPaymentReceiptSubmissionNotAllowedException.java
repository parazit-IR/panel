package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptSubmissionNotAllowedException extends RuntimeException {
    public ManualPaymentReceiptSubmissionNotAllowedException(String message) {
        super(message);
    }
}
