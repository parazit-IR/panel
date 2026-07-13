package com.parazit.panel.application.telegram.model;

import java.util.List;

public record TelegramInlineKeyboardRow(List<TelegramInlineButton> buttons) {

    public TelegramInlineKeyboardRow {
        if (buttons == null || buttons.isEmpty()) {
            throw new IllegalArgumentException("buttons must not be empty");
        }
        buttons = List.copyOf(buttons);
    }
}
