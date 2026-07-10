package com.parazit.panel.application.port.in.user;

import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;

public interface RegisterUserUseCase {

    RegisterUserResult register(RegisterUserCommand command);
}
