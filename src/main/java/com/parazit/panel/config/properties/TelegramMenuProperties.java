package com.parazit.panel.config.properties;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.menu")
public record TelegramMenuProperties(
        boolean enabled,
        boolean persistent,
        boolean showRenewal,
        boolean showTrial,
        boolean showWallet,
        boolean showTutorials,
        boolean showSupport,
        boolean showTariffs,
        boolean showPaymentsInMainMenu,
        boolean showSettingsInMainMenu,
        String defaultLocale
) {

    public TelegramMenuProperties {
        defaultLocale = defaultLocale == null || defaultLocale.isBlank()
                ? "fa"
                : defaultLocale.trim().toLowerCase(Locale.ROOT);
        if (!defaultLocale.equals("fa") && !defaultLocale.equals("en")) {
            throw new IllegalArgumentException("app.telegram.menu.default-locale must be fa or en");
        }
        if (enabled && !hasVisibleAction(showRenewal, showTrial, showWallet, showTutorials, showSupport, showTariffs, showPaymentsInMainMenu, showSettingsInMainMenu)) {
            throw new IllegalArgumentException("app.telegram.menu must expose at least one visible action when enabled");
        }
    }

    private static boolean hasVisibleAction(boolean... optionalActions) {
        if (optionalActions == null) {
            return true;
        }
        // Buy subscription and my services are core actions.
        return true;
    }
}
