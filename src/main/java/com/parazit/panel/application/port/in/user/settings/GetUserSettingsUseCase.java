package com.parazit.panel.application.port.in.user.settings;

import com.parazit.panel.application.user.settings.query.GetUserSettingsQuery;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;

public interface GetUserSettingsUseCase {

    UserSettingsResult getSettings(GetUserSettingsQuery query);
}
