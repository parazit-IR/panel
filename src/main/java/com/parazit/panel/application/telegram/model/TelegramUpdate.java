package com.parazit.panel.application.telegram.model;

import java.time.Instant;
import java.util.Objects;

public record TelegramUpdate(
        long updateId,
        TelegramUpdateType type,
        TelegramActor actor,
        TelegramChat chat,
        TelegramMessage message,
        TelegramCallbackQuery callbackQuery,
        Instant receivedAt
) {

    public TelegramUpdate {
        if (updateId < 0) {
            throw new IllegalArgumentException("updateId must be non-negative");
        }
        type = type == null ? TelegramUpdateType.UNSUPPORTED : type;
        receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
    }

    public boolean privateChat() {
        return chat != null && chat.type() == TelegramChatType.PRIVATE;
    }

    @Override
    public String toString() {
        return "TelegramUpdate[updateId=%d,type=%s]".formatted(updateId, type);
    }
}
