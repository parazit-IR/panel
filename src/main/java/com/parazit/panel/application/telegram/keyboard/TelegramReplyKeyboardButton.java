package com.parazit.panel.application.telegram.keyboard;

public record TelegramReplyKeyboardButton(String text) {

    public TelegramReplyKeyboardButton {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("reply keyboard button text must not be blank");
        }
        text = text.trim();
        if (text.length() > 64) {
            text = text.substring(0, 64);
        }
    }
}
