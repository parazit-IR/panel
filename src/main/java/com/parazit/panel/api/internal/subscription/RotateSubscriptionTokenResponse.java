package com.parazit.panel.api.internal.subscription;

import java.time.Instant;
import java.util.UUID;

public record RotateSubscriptionTokenResponse(
        UUID subscriptionId,
        String accessToken,
        String subscriptionUrl,
        String accessTokenPrefix,
        int tokenVersion,
        Instant expiresAt
) {
}
