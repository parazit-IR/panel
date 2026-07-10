package com.parazit.panel.api.internal.user.settings;

import com.parazit.panel.application.user.settings.command.UpdateUserSettingsCommand;
import com.parazit.panel.application.user.settings.query.GetUserSettingsQuery;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
import org.springframework.stereotype.Component;

@Component
public class UserSettingsApiMapper {

    public GetUserSettingsQuery toQuery(Long telegramUserId) {
        return new GetUserSettingsQuery(telegramUserId);
    }

    public UpdateUserSettingsCommand toCommand(Long telegramUserId, UpdateUserSettingsRequest request) {
        return new UpdateUserSettingsCommand(
                telegramUserId,
                request.notificationsEnabled(),
                request.renewalRemindersEnabled(),
                request.usageAlertsEnabled(),
                request.usageAlertThresholdPercent()
        );
    }

    public UserSettingsResponse toResponse(UserSettingsResult result) {
        return new UserSettingsResponse(
                result.settingsId(),
                result.userId(),
                result.telegramUserId(),
                result.notificationsEnabled(),
                result.renewalRemindersEnabled(),
                result.usageAlertsEnabled(),
                result.usageAlertThresholdPercent(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
