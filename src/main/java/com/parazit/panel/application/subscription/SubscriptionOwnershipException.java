package com.parazit.panel.application.subscription;

public class SubscriptionOwnershipException extends RuntimeException {

    public SubscriptionOwnershipException() {
        super("Subscription does not belong to user");
    }
}
