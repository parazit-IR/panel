package com.parazit.panel.api.internal.subscription;

public record SubscriptionDeliveryLinksResponse(
        String deliverySummaryEndpoint,
        String subscriptionQrEndpoint,
        int configCount
) {
}

