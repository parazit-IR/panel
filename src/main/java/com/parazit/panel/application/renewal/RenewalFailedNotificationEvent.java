package com.parazit.panel.application.renewal;

import java.time.Instant;
import java.util.UUID;

public record RenewalFailedNotificationEvent(
        UUID userId,
        long telegramUserId,
        UUID renewalOrderId,
        UUID subscriptionId,
        String serviceDisplayName,
        String serviceUsername,
        String customerFacingReasonCode,
        Instant failedAt
) {
}
