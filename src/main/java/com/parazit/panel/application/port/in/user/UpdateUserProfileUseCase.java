package com.parazit.panel.application.port.in.user;

import com.parazit.panel.application.user.command.UpdateUserProfileCommand;
import com.parazit.panel.application.user.result.UpdateUserProfileResult;

public interface UpdateUserProfileUseCase {

    UpdateUserProfileResult updateProfile(UpdateUserProfileCommand command);
}
