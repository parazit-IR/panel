package com.parazit.panel.application.telegram.result;

public record TelegramEditResult(
        long chatId,
        long messageId,
        boolean accepted
) {
}
