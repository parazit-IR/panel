package com.parazit.panel.application.telegram.service;

import com.parazit.panel.application.customer.result.CustomerServiceDetailsResult;
import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.application.customer.result.UsageFreshness;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramCustomerServiceDetailsFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramMessageFormatter formatter;
    private final TelegramCustomerTextFormatter textFormatter;

    public TelegramCustomerServiceDetailsFormatter(
            TelegramMessageCatalog catalog,
            TelegramMessageFormatter formatter,
            TelegramCustomerTextFormatter textFormatter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String format(CustomerServiceDetailsResult result, String language) {
        StringBuilder text = new StringBuilder(catalog.text(language, "telegram.services.details")).append("\n\n");
        append(text, catalog.text(language, "telegram.service.name"), formatter.html(result.serviceUsername()));
        append(text, catalog.text(language, "telegram.service.plan"), formatter.html(result.planName()));
        append(text, catalog.text(language, "telegram.service.status"), statusLabel(result.status(), language));
        if (result.totalTrafficBytes().isPresent()) {
            append(text, catalog.text(language, "telegram.service.total_traffic"), textFormatter.traffic(result.totalTrafficBytes().getAsLong(), language));
        } else if (result.usedTrafficBytes().isPresent()) {
            append(text, catalog.text(language, "telegram.service.total_traffic"), catalog.text(language, "telegram.tariffs.unlimited"));
        }
        if (result.usedTrafficBytes().isPresent()) {
            append(text, catalog.text(language, "telegram.service.used_traffic"), textFormatter.traffic(result.usedTrafficBytes().getAsLong(), language));
        }
        if (result.remainingTrafficBytes().isPresent()) {
            append(text, catalog.text(language, "telegram.service.remaining_traffic"), textFormatter.traffic(result.remainingTrafficBytes().getAsLong(), language));
        }
        if (result.usageFreshness() == UsageFreshness.UNAVAILABLE) {
            append(text, catalog.text(language, "telegram.service.usage_unavailable"), catalog.text(language, "telegram.service.usage_unavailable_text"));
        } else if (result.usageFreshness() == UsageFreshness.STALE) {
            text.append(catalog.text(language, "telegram.service.usage_stale")).append("\n\n");
        }
        result.expiresAt().ifPresent(expiry -> append(text, catalog.text(language, "telegram.service.expires_at"), formatter.formatDate(expiry)));
        result.remainingDuration().ifPresent(duration -> append(text, catalog.text(language, "telegram.service.remaining_time"), textFormatter.duration(duration, language)));
        if (result.status() == CustomerServiceStatus.PROVISIONING) {
            text.append(catalog.text(language, "telegram.service.provisioning")).append("\n\n");
        } else if (result.status() == CustomerServiceStatus.FAILED) {
            text.append(catalog.text(language, "telegram.service.failed")).append("\n\n");
        }
        return text.toString().trim();
    }

    public String statusLabel(CustomerServiceStatus status, String language) {
        return catalog.text(language, "telegram.service.status." + status.name());
    }

    private static void append(StringBuilder text, String label, String value) {
        text.append(label).append(":\n").append(value).append("\n\n");
    }
}
