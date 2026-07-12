package com.parazit.panel.application.payment.manual;

public class ManualPaymentDestinationUnavailableException extends RuntimeException {

    public ManualPaymentDestinationUnavailableException() {
        super("Manual payment destination is unavailable");
    }
}
