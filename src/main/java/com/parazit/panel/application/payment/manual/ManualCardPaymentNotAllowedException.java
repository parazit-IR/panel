package com.parazit.panel.application.payment.manual;

public class ManualCardPaymentNotAllowedException extends RuntimeException {

    public ManualCardPaymentNotAllowedException(String message) {
        super(message);
    }
}
