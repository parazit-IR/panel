package com.parazit.panel.application.telegram.model;

import java.time.Instant;

public record TelegramMessage(
        long messageId,
        String text,
        Instant sentAt
) {

    public TelegramMessage {
        if (messageId < 0) {
            throw new IllegalArgumentException("messageId must be non-negative");
        }
        text = text == null ? "" : text;
    }
}
