package com.parazit.panel.application.port.in.user;

import com.parazit.panel.application.user.command.ChangeUserLanguageCommand;
import com.parazit.panel.application.user.result.UserLanguageResult;

public interface ChangeUserLanguageUseCase {

    UserLanguageResult changeLanguage(ChangeUserLanguageCommand command);
}
