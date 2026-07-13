package com.parazit.panel.config.properties;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.support")
public record SupportProperties(
        boolean enabled,
        String displayName,
        String telegramUsername,
        String email,
        String workingHoursText,
        String responseTimeText,
        boolean showFaq,
        boolean showDirectMessage,
        boolean showEmail,
        String unavailableMessage
) {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{5,32}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public SupportProperties {
        displayName = normalize(displayName, 80);
        telegramUsername = normalizeUsername(telegramUsername);
        email = normalize(email, 120);
        workingHoursText = normalize(workingHoursText, 160);
        responseTimeText = normalize(responseTimeText, 160);
        unavailableMessage = normalize(unavailableMessage, 300);
        if (enabled && !showFaq && !showDirectMessage && !showEmail) {
            throw new IllegalArgumentException("app.telegram.support must expose at least one channel when enabled");
        }
        if (enabled && showDirectMessage && telegramUsername.isBlank()) {
            throw new IllegalArgumentException("app.telegram.support.telegram-username is required");
        }
        if (!telegramUsername.isBlank() && !USERNAME.matcher(telegramUsername).matches()) {
            throw new IllegalArgumentException("app.telegram.support.telegram-username is invalid");
        }
        if (showEmail && !email.isBlank() && !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("app.telegram.support.email is invalid");
        }
    }

    public URI telegramUrl() {
        if (telegramUsername.isBlank()) {
            return null;
        }
        return URI.create("https://t.me/" + telegramUsername);
    }

    private static String normalizeUsername(String value) {
        String normalized = normalize(value, 32);
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new IllegalArgumentException("support configuration value is too long");
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "SupportProperties[enabled=%s,displayName=%s,showFaq=%s,showDirectMessage=%s,showEmail=%s]"
                .formatted(enabled, displayName, showFaq, showDirectMessage, showEmail);
    }
}
