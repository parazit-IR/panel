package com.parazit.panel.application.renewal;

import java.time.Instant;
import java.util.UUID;

public record RenewalQueuedNotificationEvent(
        UUID userId,
        long telegramUserId,
        UUID renewalOrderId,
        UUID targetSubscriptionId,
        String serviceDisplayName,
        String serviceUsername,
        Instant queuedAt
) {
}
