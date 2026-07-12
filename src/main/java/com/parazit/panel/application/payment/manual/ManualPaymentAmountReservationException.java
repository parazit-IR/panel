package com.parazit.panel.application.payment.manual;

public class ManualPaymentAmountReservationException extends RuntimeException {

    public ManualPaymentAmountReservationException() {
        super("Could not reserve a unique manual payment amount");
    }
}
