package com.parazit.panel.application.subscription;

import java.util.UUID;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(UUID subscriptionId) {
        super("Subscription not found: " + subscriptionId);
    }
}
