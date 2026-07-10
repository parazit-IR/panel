package com.parazit.panel.api.internal.user.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateUserSettingsRequest(
        @NotNull
        Boolean notificationsEnabled,

        @NotNull
        Boolean renewalRemindersEnabled,

        @NotNull
        Boolean usageAlertsEnabled,

        @NotNull
        @Min(1)
        @Max(100)
        Integer usageAlertThresholdPercent
) {
}
