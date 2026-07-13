package com.parazit.panel.application.telegram.service;

import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramMyServicesMessageFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramCustomerServiceDetailsFormatter detailsFormatter;
    private final TelegramCustomerTextFormatter textFormatter;

    public TelegramMyServicesMessageFormatter(
            TelegramMessageCatalog catalog,
            TelegramCustomerServiceDetailsFormatter detailsFormatter,
            TelegramCustomerTextFormatter textFormatter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.detailsFormatter = Objects.requireNonNull(detailsFormatter, "detailsFormatter must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String listText(String language) {
        return catalog.text(language, "telegram.services.title") + "\n\n" + catalog.text(language, "telegram.services.description");
    }

    public String buttonLabel(CustomerServiceSummaryResult service, String language) {
        String base = detailsFormatter.statusLabel(service.status(), language) + " " + service.serviceUsername();
        if (service.remainingDuration().isPresent()) {
            base = base + " — " + textFormatter.duration(service.remainingDuration().get(), language);
        }
        return base.length() <= 64 ? base : base.substring(0, 64);
    }
}
