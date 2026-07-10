package com.parazit.panel.api.internal.user;

import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import java.time.Instant;

public record UserProfileResponse(
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
}
