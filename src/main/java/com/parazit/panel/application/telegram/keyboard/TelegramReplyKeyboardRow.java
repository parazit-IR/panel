package com.parazit.panel.application.telegram.keyboard;

import java.util.List;

public record TelegramReplyKeyboardRow(List<TelegramReplyKeyboardButton> buttons) {

    public TelegramReplyKeyboardRow {
        if (buttons == null || buttons.isEmpty()) {
            throw new IllegalArgumentException("reply keyboard row buttons must not be empty");
        }
        buttons = List.copyOf(buttons);
    }
}
