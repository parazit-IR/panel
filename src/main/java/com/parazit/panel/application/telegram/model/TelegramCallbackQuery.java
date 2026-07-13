package com.parazit.panel.application.telegram.model;

public record TelegramCallbackQuery(
        String callbackQueryId,
        long sourceMessageId,
        String data
) {

    public TelegramCallbackQuery {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            throw new IllegalArgumentException("callbackQueryId must not be blank");
        }
        if (sourceMessageId < 0) {
            throw new IllegalArgumentException("sourceMessageId must be non-negative");
        }
        data = data == null ? "" : data;
    }

    @Override
    public String toString() {
        return "TelegramCallbackQuery[id=%s,sourceMessageId=%d]".formatted(callbackQueryId, sourceMessageId);
    }
}
