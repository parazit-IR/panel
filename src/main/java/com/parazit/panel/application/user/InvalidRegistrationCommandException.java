package com.parazit.panel.application.user;

public class InvalidRegistrationCommandException extends IllegalArgumentException {

    public InvalidRegistrationCommandException(String message) {
        super(message);
    }
}
