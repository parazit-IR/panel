package com.parazit.panel.application.user.result;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import java.time.Instant;

public record UserProfileResult(
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        UserLanguage language,
        UserStatus status,
        boolean blocked,
        Instant createdAt,
        Instant updatedAt,
        Instant lastInteractionAt
) {

    public static UserProfileResult from(User user) {
        return new UserProfileResult(
                user.getTelegramUserId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getLanguage(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getBlocked()),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastInteractionAt()
        );
    }
}
