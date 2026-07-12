package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptAlreadySubmittedException extends RuntimeException {
    public ManualPaymentReceiptAlreadySubmittedException() {
        super("A receipt is already active for this payment instruction");
    }
}
