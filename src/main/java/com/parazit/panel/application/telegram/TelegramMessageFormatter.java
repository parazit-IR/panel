package com.parazit.panel.application.telegram;

import com.parazit.panel.config.properties.TelegramBotProperties;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final TelegramBotProperties properties;
    private final TelegramHtmlEscaper escaper;

    public TelegramMessageFormatter(TelegramBotProperties properties, TelegramHtmlEscaper escaper) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.escaper = Objects.requireNonNull(escaper, "escaper must not be null");
    }

    public String html(String value) {
        String escaped = escaper.escape(value);
        if (escaped.length() > properties.maxMessageLength()) {
            throw new IllegalArgumentException("Telegram message is too long");
        }
        return escaped;
    }

    public String rawSensitive(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sensitive Telegram message must not be blank");
        }
        if (value.length() > properties.maxMessageLength()) {
            throw new IllegalArgumentException("Telegram sensitive message is too long");
        }
        return value;
    }

    public String formatDate(Instant instant) {
        if (instant == null) {
            return "unlimited";
        }
        return DATE_FORMATTER.format(instant);
    }
}
