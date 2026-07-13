package com.parazit.panel.application.telegram.result;

import java.time.Instant;

public record TelegramSendResult(
        long chatId,
        long messageId,
        Instant sentAt,
        TelegramMessageKind kind,
        boolean accepted
) {
}
