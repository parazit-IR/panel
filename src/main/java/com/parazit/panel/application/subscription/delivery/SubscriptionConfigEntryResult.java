package com.parazit.panel.application.subscription.delivery;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionConfigEntryResult(
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

