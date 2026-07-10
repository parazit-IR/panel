package com.parazit.panel.application.user.settings.result;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.settings.UserSettings;
import java.time.Instant;
import java.util.UUID;

public record UserSettingsResult(
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

    public static UserSettingsResult from(User user, UserSettings settings) {
        return new UserSettingsResult(
                settings.getId(),
                settings.getUserId(),
                user.getTelegramUserId(),
                settings.isNotificationsEnabled(),
                settings.isRenewalRemindersEnabled(),
                settings.isUsageAlertsEnabled(),
                settings.getUsageAlertThresholdPercent(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}
