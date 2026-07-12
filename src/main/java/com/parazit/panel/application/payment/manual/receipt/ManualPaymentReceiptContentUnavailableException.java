package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptContentUnavailableException extends RuntimeException {
    public ManualPaymentReceiptContentUnavailableException() {
        super("Manual payment receipt content is unavailable");
    }
}
