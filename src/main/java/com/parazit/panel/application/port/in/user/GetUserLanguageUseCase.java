package com.parazit.panel.application.port.in.user;

import com.parazit.panel.application.user.query.GetUserLanguageQuery;
import com.parazit.panel.application.user.result.UserLanguageResult;

public interface GetUserLanguageUseCase {

    UserLanguageResult getLanguage(GetUserLanguageQuery query);
}
