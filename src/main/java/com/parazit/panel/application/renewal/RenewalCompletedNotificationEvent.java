package com.parazit.panel.application.renewal;

import java.time.Instant;
import java.util.UUID;

public record RenewalCompletedNotificationEvent(
        UUID userId,
        long telegramUserId,
        UUID renewalOrderId,
        UUID subscriptionId,
        String serviceDisplayName,
        String serviceUsername,
        Instant newExpiryAt,
        long newTrafficLimitBytes,
        Instant completedAt
) {
}
