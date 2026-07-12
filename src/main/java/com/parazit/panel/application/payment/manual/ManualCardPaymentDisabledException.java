package com.parazit.panel.application.payment.manual;

public class ManualCardPaymentDisabledException extends RuntimeException {

    public ManualCardPaymentDisabledException() {
        super("Manual card payment is disabled");
    }
}
