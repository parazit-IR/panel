package com.parazit.panel.api.internal.user;

import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import org.springframework.stereotype.Component;

@Component
public class RegisterUserApiMapper {

    public RegisterUserCommand toCommand(RegisterUserRequest request) {
        return new RegisterUserCommand(
                request.telegramUserId(),
                request.username(),
                request.firstName(),
                request.lastName(),
                request.languageCode()
        );
    }

    public RegisterUserResponse toResponse(RegisterUserResult result) {
        return new RegisterUserResponse(
                result.userId(),
                result.telegramUserId(),
                result.username(),
                result.firstName(),
                result.lastName(),
                result.language(),
                result.status(),
                result.blocked(),
                result.newlyCreated(),
                result.registeredAt(),
                result.lastInteractionAt()
        );
    }
}
