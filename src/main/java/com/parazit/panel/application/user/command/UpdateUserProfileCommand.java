package com.parazit.panel.application.user.command;

import com.parazit.panel.domain.user.UserLanguage;

public record UpdateUserProfileCommand(
        Long telegramUserId,
        String firstName,
        String lastName,
        UserLanguage language
) {
}
