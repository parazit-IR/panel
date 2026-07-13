package com.parazit.panel.application.telegram.support;

import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TelegramSupportUrlFactory {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{5,32}");

    public URI create(String username) {
        String normalized = normalize(username);
        if (!USERNAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Telegram support username is invalid");
        }
        return URI.create("https://t.me/" + normalized);
    }

    public String normalize(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Telegram support username is required");
        }
        String normalized = username.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }
}
