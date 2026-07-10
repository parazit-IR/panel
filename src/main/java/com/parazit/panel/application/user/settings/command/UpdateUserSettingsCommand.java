package com.parazit.panel.application.user.settings.command;

public record UpdateUserSettingsCommand(
        Long telegramUserId,
        boolean notificationsEnabled,
        boolean renewalRemindersEnabled,
        boolean usageAlertsEnabled,
        Integer usageAlertThresholdPercent
) {
}
