package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramUnavailableFeatureFormatter {

    private final TelegramMessageCatalog catalog;

    public TelegramUnavailableFeatureFormatter(TelegramMessageCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    public String format(String language, TelegramMenuFeatureAvailability availability) {
        String key = availability == null || availability.unavailableMessageKey() == null
                ? "telegram.feature.unavailable"
                : availability.unavailableMessageKey();
        return catalog.text(language, key);
    }
}
