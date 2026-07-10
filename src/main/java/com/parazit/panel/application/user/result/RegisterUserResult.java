package com.parazit.panel.application.user.result;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisterUserResult(
        UUID userId,
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        UserLanguage language,
        UserStatus status,
        boolean blocked,
        boolean newlyCreated,
        Instant registeredAt,
        Instant lastInteractionAt
) {

    public static RegisterUserResult from(User user, boolean newlyCreated) {
        return new RegisterUserResult(
                user.getId(),
                user.getTelegramUserId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getLanguage(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getBlocked()),
                newlyCreated,
                user.getCreatedAt(),
                user.getLastInteractionAt()
        );
    }
}
