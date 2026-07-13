package com.parazit.panel.application.telegram.command;

import com.parazit.panel.application.telegram.model.TelegramInlineKeyboard;
import com.parazit.panel.application.telegram.model.TelegramParseMode;

public record SendTelegramMessageCommand(
        long chatId,
        String text,
        TelegramParseMode parseMode,
        TelegramInlineKeyboard keyboard,
        boolean disableLinkPreview,
        Long replyToMessageId
) {

    public SendTelegramMessageCommand {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("message text must not be blank");
        }
        parseMode = parseMode == null ? TelegramParseMode.NONE : parseMode;
        keyboard = keyboard == null ? TelegramInlineKeyboard.empty() : keyboard;
    }

    @Override
    public String toString() {
        return "SendTelegramMessageCommand[chatId=%d,parseMode=%s]".formatted(chatId, parseMode);
    }
}
