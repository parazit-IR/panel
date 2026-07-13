package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record SubscriptionDeliveryContent(
        UUID subscriptionId,
        UUID userId,
        UUID provisionId,
        SubscriptionStatus status,
        String planName,
        Instant expiresAt,
        String accessTokenPrefix,
        int tokenVersion,
        List<ResolvedSubscriptionConfigEntry> entries
) {

    SubscriptionDeliveryContent {
        entries = List.copyOf(entries);
    }
}

