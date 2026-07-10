package com.parazit.panel.application.user;

import java.util.NoSuchElementException;

public class UserProfileNotFoundException extends NoSuchElementException {

    public UserProfileNotFoundException(Long telegramUserId) {
        super("User profile not found for telegramUserId " + telegramUserId);
    }
}
