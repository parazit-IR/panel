package com.parazit.panel.application.subscription;

public class SubscriptionTokenInvalidException extends RuntimeException {

    public SubscriptionTokenInvalidException() {
        super("Subscription token is invalid");
    }
}
