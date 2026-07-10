package com.parazit.panel.api.internal.user;

import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisterUserResponse(
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
}
