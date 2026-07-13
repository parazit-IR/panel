package com.parazit.panel.application.telegram.model;

import java.util.List;

public record TelegramInlineKeyboard(List<TelegramInlineKeyboardRow> rows) {

    public TelegramInlineKeyboard {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public static TelegramInlineKeyboard empty() {
        return new TelegramInlineKeyboard(List.of());
    }

    public static TelegramInlineKeyboard ofRows(List<TelegramInlineKeyboardRow> rows) {
        return new TelegramInlineKeyboard(rows);
    }
}
