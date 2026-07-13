package com.parazit.panel.application.telegram.model;

public record TelegramChat(
        long chatId,
        TelegramChatType type,
        String title
) {

    public TelegramChat {
        type = type == null ? TelegramChatType.UNKNOWN : type;
        title = title == null || title.isBlank() ? null : title.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
