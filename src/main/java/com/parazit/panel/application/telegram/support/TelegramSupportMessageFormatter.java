package com.parazit.panel.application.telegram.support;

import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.config.properties.SupportProperties;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramSupportMessageFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper escaper;

    public TelegramSupportMessageFormatter(TelegramMessageCatalog catalog, TelegramHtmlEscaper escaper) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.escaper = Objects.requireNonNull(escaper, "escaper must not be null");
    }

    public String format(String language, SupportProperties properties) {
        if (!properties.enabled()) {
            return properties.unavailableMessage().isBlank()
                    ? catalog.text(language, "telegram.support.unavailable")
                    : escaper.escape(properties.unavailableMessage());
        }
        StringBuilder builder = new StringBuilder(catalog.text(language, "telegram.support.title"))
                .append("\n\n")
                .append(catalog.text(language, "telegram.support.description"));
        if (!properties.displayName().isBlank()) {
            builder.append("\n\n").append(escaper.escape(properties.displayName()));
        }
        if (!properties.workingHoursText().isBlank()) {
            builder.append("\n\n")
                    .append(catalog.text(language, "telegram.support.working_hours"))
                    .append(":\n")
                    .append(escaper.escape(properties.workingHoursText()));
        }
        if (!properties.responseTimeText().isBlank()) {
            builder.append("\n\n").append(escaper.escape(properties.responseTimeText()));
        }
        return builder.toString();
    }
}
