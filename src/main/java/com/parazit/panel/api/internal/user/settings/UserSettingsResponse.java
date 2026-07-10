package com.parazit.panel.api.internal.user.settings;

import java.time.Instant;
import java.util.UUID;

public record UserSettingsResponse(
        UUID settingsId,
        UUID userId,
        Long telegramUserId,
        boolean notificationsEnabled,
        boolean renewalRemindersEnabled,
        boolean usageAlertsEnabled,
        int usageAlertThresholdPercent,
        Instant createdAt,
        Instant updatedAt
) {
}
