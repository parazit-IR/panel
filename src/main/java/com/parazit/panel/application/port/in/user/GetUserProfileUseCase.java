package com.parazit.panel.application.port.in.user;

import com.parazit.panel.application.user.query.GetUserProfileQuery;
import com.parazit.panel.application.user.result.UserProfileResult;

public interface GetUserProfileUseCase {

    UserProfileResult getProfile(GetUserProfileQuery query);
}
