package com.parazit.panel.application.subscription.result;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateSubscriptionResult(
        UUID subscriptionId,
        UUID userId,
        UUID orderId,
        UUID xuiClientProvisionId,
        SubscriptionStatus status,
        String rawAccessToken,
        String accessTokenPrefix,
        int tokenVersion,
        Instant activatedAt,
        Instant expiresAt,
        boolean newlyCreated
) {
}
