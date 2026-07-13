package com.parazit.panel.application.telegram.model;

public record TelegramInlineButton(
        String text,
        TelegramInlineButtonType type,
        String value
) {

    public TelegramInlineButton {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("button text must not be blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("button value must not be blank");
        }
        type = type == null ? TelegramInlineButtonType.CALLBACK : type;
        text = text.trim();
        if (text.length() > 64) {
            text = text.substring(0, 64);
        }
    }

    public static TelegramInlineButton callback(String text, String callbackData) {
        return new TelegramInlineButton(text, TelegramInlineButtonType.CALLBACK, callbackData);
    }

    public static TelegramInlineButton url(String text, String url) {
        return new TelegramInlineButton(text, TelegramInlineButtonType.URL, url);
    }

    public static TelegramInlineButton copyText(String text, String value) {
        return new TelegramInlineButton(text, TelegramInlineButtonType.COPY_TEXT, value);
    }

    @Override
    public String toString() {
        return "TelegramInlineButton[type=%s,text=%s]".formatted(type, text);
    }
}
