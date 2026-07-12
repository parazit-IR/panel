package com.parazit.panel.application.payment;

public class PaymentConflictException extends RuntimeException {

    public PaymentConflictException(String message) {
        super(message);
    }
}
