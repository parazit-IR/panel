package com.parazit.panel.application.subscription;

public class SubscriptionNotAccessibleException extends RuntimeException {

    public SubscriptionNotAccessibleException() {
        super("Subscription is not available");
    }

    public SubscriptionNotAccessibleException(String message) {
        super(message);
    }
}
