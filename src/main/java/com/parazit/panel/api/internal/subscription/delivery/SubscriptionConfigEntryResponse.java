package com.parazit.panel.api.internal.subscription.delivery;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionConfigEntryResponse(
        UUID subscriptionId,
        int index,
        String protocol,
        String displayName,
        String uri,
        String server,
        int port,
        String transport,
        String security,
        Instant expiresAt
) {
}

