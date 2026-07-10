package com.parazit.panel.application.user.command;

public record UpdateUserProfileCommand(
        Long telegramUserId,
        String firstName,
        String lastName
) {
}
