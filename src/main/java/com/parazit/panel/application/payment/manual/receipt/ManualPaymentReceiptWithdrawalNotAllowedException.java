package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptWithdrawalNotAllowedException extends RuntimeException {
    public ManualPaymentReceiptWithdrawalNotAllowedException(String message) {
        super(message);
    }
}
