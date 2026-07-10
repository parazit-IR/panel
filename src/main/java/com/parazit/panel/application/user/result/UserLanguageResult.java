package com.parazit.panel.application.user.result;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import java.time.Instant;
import java.util.UUID;

public record UserLanguageResult(
        UUID userId,
        Long telegramUserId,
        UserLanguage language,
        Instant updatedAt
) {

    public static UserLanguageResult from(User user) {
        return new UserLanguageResult(
                user.getId(),
                user.getTelegramUserId(),
                user.getLanguage(),
                user.getUpdatedAt()
        );
    }
}
