package com.parazit.panel.application.telegram.menu;

public record TelegramMenuFeatureAvailability(
        boolean visible,
        boolean enabled,
        String unavailableMessageKey
) {

    public static TelegramMenuFeatureAvailability available() {
        return new TelegramMenuFeatureAvailability(true, true, null);
    }

    public static TelegramMenuFeatureAvailability hidden() {
        return new TelegramMenuFeatureAvailability(false, false, "telegram.feature.unavailable");
    }

    public static TelegramMenuFeatureAvailability unavailable(String messageKey) {
        return new TelegramMenuFeatureAvailability(true, false, messageKey);
    }
}
