package com.parazit.panel.application.telegram.model;

public record TelegramActor(
        long telegramUserId,
        String username,
        String firstName,
        String lastName,
        String languageCode,
        boolean bot
) {

    public TelegramActor {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        username = bounded(username, 64);
        firstName = bounded(firstName == null || firstName.isBlank() ? "Telegram User" : firstName, 128);
        lastName = bounded(lastName, 128);
        languageCode = bounded(languageCode, 16);
    }

    @Override
    public String toString() {
        return "TelegramActor[telegramUserId=%d,bot=%s]".formatted(telegramUserId, bot);
    }

    private static String bounded(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
