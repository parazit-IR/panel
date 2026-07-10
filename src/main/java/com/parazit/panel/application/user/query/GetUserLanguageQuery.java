package com.parazit.panel.application.user.query;

import java.util.Objects;

public record GetUserLanguageQuery(
        Long telegramUserId
) {

    public GetUserLanguageQuery {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }
}
