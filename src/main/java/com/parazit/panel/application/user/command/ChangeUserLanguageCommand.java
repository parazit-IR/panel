package com.parazit.panel.application.user.command;

import java.util.Objects;

public record ChangeUserLanguageCommand(
        Long telegramUserId,
        String languageCode
) {

    public ChangeUserLanguageCommand {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        Objects.requireNonNull(languageCode, "languageCode must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }
}
