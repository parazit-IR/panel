package com.parazit.panel.api.internal.subscription.delivery;

import com.parazit.panel.domain.subscription.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDeliverySummaryResponse(
        UUID subscriptionId,
        String planName,
        SubscriptionStatus status,
        Instant expiresAt,
        int tokenVersion,
        String accessTokenPrefix,
        int configCount,
        List<SubscriptionDeliveryEntryResponse> entries,
        boolean subscriptionUrlAvailable,
        boolean subscriptionQrAvailable,
        boolean configQrAvailable
) {
}

