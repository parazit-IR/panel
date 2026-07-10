package com.parazit.panel.application.port.in.user.settings;

import com.parazit.panel.application.user.settings.command.UpdateUserSettingsCommand;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;

public interface UpdateUserSettingsUseCase {

    UserSettingsResult updateSettings(UpdateUserSettingsCommand command);
}
