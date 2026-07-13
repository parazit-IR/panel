package com.parazit.panel.api.internal.subscription.delivery;

public record SubscriptionDeliveryEntryResponse(
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

