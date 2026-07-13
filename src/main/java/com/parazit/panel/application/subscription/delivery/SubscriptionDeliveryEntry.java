package com.parazit.panel.application.subscription.delivery;

public record SubscriptionDeliveryEntry(
        int index,
        String protocol,
        String displayName,
        String maskedServer,
        int port,
        String transport,
        String security,
        boolean qrAvailable
) {
}

