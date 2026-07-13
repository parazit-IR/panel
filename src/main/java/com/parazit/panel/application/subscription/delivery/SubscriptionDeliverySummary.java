package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDeliverySummary(
        UUID subscriptionId,
        String planName,
        SubscriptionStatus status,
        Instant expiresAt,
        int tokenVersion,
        String accessTokenPrefix,
        int configCount,
        List<SubscriptionDeliveryEntry> entries,
        boolean subscriptionUrlAvailable,
        boolean subscriptionQrAvailable,
        boolean configQrAvailable
) {

    public SubscriptionDeliverySummary {
        entries = List.copyOf(entries);
    }
}

