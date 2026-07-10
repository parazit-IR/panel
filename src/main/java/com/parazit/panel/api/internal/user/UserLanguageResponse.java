package com.parazit.panel.api.internal.user;

import com.parazit.panel.domain.user.UserLanguage;
import java.time.Instant;
import java.util.UUID;

public record UserLanguageResponse(
        UUID userId,
        Long telegramUserId,
        UserLanguage language,
        Instant updatedAt
) {
}
