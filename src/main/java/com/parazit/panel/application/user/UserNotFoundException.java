package com.parazit.panel.application.user;

import java.util.NoSuchElementException;

public class UserNotFoundException extends NoSuchElementException {

    public UserNotFoundException(Long telegramUserId) {
        super("User not found for telegramUserId " + telegramUserId);
    }
}
