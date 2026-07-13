package com.parazit.panel.application.telegram.keyboard;

import java.util.List;

public record TelegramReplyKeyboard(
        List<TelegramReplyKeyboardRow> rows,
        boolean resizeKeyboard,
        boolean oneTimeKeyboard,
        boolean persistent,
        boolean selective,
        String inputFieldPlaceholder
) {

    public TelegramReplyKeyboard {
        rows = rows == null ? List.of() : List.copyOf(rows);
        if (inputFieldPlaceholder != null) {
            inputFieldPlaceholder = inputFieldPlaceholder.trim();
            if (inputFieldPlaceholder.length() > 64) {
                inputFieldPlaceholder = inputFieldPlaceholder.substring(0, 64);
            }
        }
    }

    public boolean empty() {
        return rows.isEmpty();
    }

    public static TelegramReplyKeyboard none() {
        return new TelegramReplyKeyboard(List.of(), false, false, false, false, null);
    }
}
