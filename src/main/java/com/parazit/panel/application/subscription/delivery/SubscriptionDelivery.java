package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDelivery(
        UUID subscriptionId,
        SubscriptionStatus status,
        String planName,
        Instant expiresAt,
        String accessTokenPrefix,
        int tokenVersion,
        String subscriptionUrl,
        List<SubscriptionDeliveryEntry> entries
) {

    public SubscriptionDelivery {
        entries = List.copyOf(entries);
    }
}

