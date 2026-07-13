package com.parazit.panel.application.telegram.command;

public record AnswerTelegramCallbackCommand(
        String callbackQueryId,
        String text,
        boolean showAlert
) {

    public AnswerTelegramCallbackCommand {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            throw new IllegalArgumentException("callbackQueryId must not be blank");
        }
        text = text == null ? "" : text.replace('\r', ' ').replace('\n', ' ').trim();
        if (text.length() > 200) {
            text = text.substring(0, 200);
        }
    }
}
