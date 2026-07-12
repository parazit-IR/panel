package com.parazit.panel.domain.payment.manual;

public class InvalidBankCardNumberException extends RuntimeException {

    public InvalidBankCardNumberException(String message) {
        super(message);
    }
}
