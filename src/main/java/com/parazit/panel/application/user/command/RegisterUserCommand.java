package com.parazit.panel.application.user.command;

import java.util.Objects;

public record RegisterUserCommand(
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        String languageCode
) {

    public RegisterUserCommand {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        Objects.requireNonNull(firstName, "firstName must not be null");
    }
}
