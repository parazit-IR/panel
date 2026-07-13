package com.parazit.panel.api.internal.subscription;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID subscriptionId,
        UUID orderId,
        UUID xuiClientProvisionId,
        String planName,
        SubscriptionStatus status,
        String accessTokenPrefix,
        int tokenVersion,
        Instant activatedAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant lastAccessedAt,
        long accessCount,
        boolean accessible
) {
}
