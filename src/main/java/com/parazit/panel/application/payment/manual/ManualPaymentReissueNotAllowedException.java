package com.parazit.panel.application.payment.manual;

public class ManualPaymentReissueNotAllowedException extends RuntimeException {

    public ManualPaymentReissueNotAllowedException(String message) {
        super(message);
    }
}
