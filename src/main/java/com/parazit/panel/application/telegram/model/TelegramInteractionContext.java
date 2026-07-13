package com.parazit.panel.application.telegram.model;

import java.time.Instant;

public record TelegramInteractionContext(
        long updateId,
        long telegramUserId,
        long chatId,
        TelegramChatType chatType,
        String language,
        String firstName,
        Long sourceMessageId,
        String callbackQueryId,
        Instant receivedAt
) {

    public TelegramInteractionContext {
        if (updateId < 0) {
            throw new IllegalArgumentException("updateId must be non-negative");
        }
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        chatType = chatType == null ? TelegramChatType.UNKNOWN : chatType;
        language = language == null || language.isBlank() ? "EN" : language.trim().toUpperCase();
        firstName = firstName == null ? "" : firstName.trim();
    }

    public boolean privateChat() {
        return chatType == TelegramChatType.PRIVATE;
    }
}
