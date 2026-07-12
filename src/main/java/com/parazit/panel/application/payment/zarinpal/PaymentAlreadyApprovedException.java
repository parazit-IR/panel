package com.parazit.panel.application.payment.zarinpal;

public class PaymentAlreadyApprovedException extends RuntimeException {
    public PaymentAlreadyApprovedException() {
        super("Payment is already approved");
    }
}
