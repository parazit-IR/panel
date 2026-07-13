package com.parazit.panel.application.telegram.tariff;

import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramTariffMessageFormatter {

    private static final long GIB = 1024L * 1024L * 1024L;

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper escaper;
    private final TelegramPersianTextFormatter textFormatter;

    public TelegramTariffMessageFormatter(
            TelegramMessageCatalog catalog,
            TelegramHtmlEscaper escaper,
            TelegramPersianTextFormatter textFormatter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.escaper = Objects.requireNonNull(escaper, "escaper must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String format(String language, TelegramTariffPage page) {
        Objects.requireNonNull(page, "page must not be null");
        List<AvailablePlanResult> plans = page.plans();
        if (plans.isEmpty()) {
            return catalog.text(language, "telegram.tariffs.empty");
        }
        StringBuilder builder = new StringBuilder(catalog.text(language, "telegram.tariffs.title"));
        for (AvailablePlanResult plan : plans) {
            builder.append("\n\n🔹 ")
                    .append(displayName(plan))
                    .append(" — ")
                    .append(traffic(plan, language))
                    .append("\n")
                    .append(textFormatter.formatAmount(plan.priceAmount(), plan.currency().name(), language));
            if (plan.maxDevices() != null && plan.maxDevices() > 0) {
                builder.append("\n")
                        .append(catalog.text(language, "telegram.tariffs.devices"))
                        .append(": ")
                        .append(textFormatter.formatNumber(plan.maxDevices(), language));
            }
        }
        if (page.totalPages() > 1) {
            builder.append("\n\n")
                    .append(textFormatter.formatNumber(page.page(), language))
                    .append(" / ")
                    .append(textFormatter.formatNumber(page.totalPages(), language));
        }
        return builder.toString();
    }

    private String displayName(AvailablePlanResult plan) {
        String name = plan.name() == null || plan.name().isBlank() ? plan.code() : plan.name();
        return escaper.escape(name);
    }

    private String traffic(AvailablePlanResult plan, String language) {
        if (plan.trafficLimitBytes() == null) {
            return catalog.text(language, "telegram.tariffs.unlimited");
        }
        long gib = Math.max(1L, Math.round(plan.trafficLimitBytes() / (double) GIB));
        return textFormatter.formatNumber(gib, language) + " " + catalog.text(language, "telegram.tariffs.gigabyte");
    }
}
