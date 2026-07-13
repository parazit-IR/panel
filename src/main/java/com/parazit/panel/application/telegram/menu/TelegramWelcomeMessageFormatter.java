package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramWelcomeMessageFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper escaper;
    private final TelegramBotProperties properties;

    public TelegramWelcomeMessageFormatter(
            TelegramMessageCatalog catalog,
            TelegramHtmlEscaper escaper,
            TelegramBotProperties properties
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.escaper = Objects.requireNonNull(escaper, "escaper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public String format(String language, String firstName) {
        String safeName = firstName == null || firstName.isBlank()
                ? catalog.text(language, "telegram.welcome.generic_name")
                : escaper.escape(firstName.trim());
        String text = catalog.text(language, "telegram.welcome.title").replace("{firstName}", safeName)
                + "\n\n"
                + catalog.text(language, "telegram.welcome.body");
        if (text.length() > properties.maxMessageLength()) {
            return text.substring(0, properties.maxMessageLength());
        }
        return text;
    }
}
