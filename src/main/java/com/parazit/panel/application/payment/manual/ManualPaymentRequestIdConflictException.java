package com.parazit.panel.application.payment.manual;

public class ManualPaymentRequestIdConflictException extends RuntimeException {

    public ManualPaymentRequestIdConflictException() {
        super("instructionRequestId belongs to a different payment");
    }
}
