package com.parazit.panel.application.telegram.command;

import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramParseMode;

public record EditTelegramMessageCommand(
        long chatId,
        long messageId,
        String text,
        TelegramParseMode parseMode,
        TelegramInlineKeyboard keyboard
) {

    public EditTelegramMessageCommand {
        if (messageId < 0) {
            throw new IllegalArgumentException("messageId must be non-negative");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("message text must not be blank");
        }
        parseMode = parseMode == null ? TelegramParseMode.NONE : parseMode;
        keyboard = keyboard == null ? TelegramInlineKeyboard.empty() : keyboard;
    }
}
