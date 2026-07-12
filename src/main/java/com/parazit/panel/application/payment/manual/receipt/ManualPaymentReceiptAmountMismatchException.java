package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptAmountMismatchException extends RuntimeException {
    public ManualPaymentReceiptAmountMismatchException() {
        super("Claimed amount must exactly match the manual payment instruction amount");
    }
}
