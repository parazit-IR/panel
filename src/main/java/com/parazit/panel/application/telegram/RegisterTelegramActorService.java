package com.parazit.panel.application.telegram;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.telegram.model.TelegramActor;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RegisterTelegramActorService {

    private final RegisterUserUseCase registerUserUseCase;

    public RegisterTelegramActorService(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = Objects.requireNonNull(registerUserUseCase, "registerUserUseCase must not be null");
    }

    public RegisterUserResult registerOrRefresh(TelegramActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        if (actor.bot()) {
            throw new IllegalArgumentException("bot actors cannot be registered");
        }
        return registerUserUseCase.register(new RegisterUserCommand(
                actor.telegramUserId(),
                actor.username(),
                actor.firstName(),
                actor.lastName(),
                actor.languageCode()
        ));
    }
}
