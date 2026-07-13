package com.parazit.panel.application.subscription.model;

public record SubscriptionConfigEntry(
        VlessSubscriptionConfig vless
) {

    public SubscriptionConfigEntry {
        if (vless == null) {
            throw new IllegalArgumentException("vless config is required");
        }
    }
}
