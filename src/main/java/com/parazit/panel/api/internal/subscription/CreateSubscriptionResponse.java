package com.parazit.panel.api.internal.subscription;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateSubscriptionResponse(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID xuiClientProvisionId,
        SubscriptionStatus status,
        String accessToken,
        String subscriptionUrl,
        String accessTokenPrefix,
        int tokenVersion,
        Instant activatedAt,
        Instant expiresAt,
        boolean newlyCreated,
        SubscriptionDeliveryLinksResponse delivery
) {
}
