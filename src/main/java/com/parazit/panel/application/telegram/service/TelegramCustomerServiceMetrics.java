package com.parazit.panel.application.telegram.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramCustomerServiceMetrics {

    private final MeterRegistry meterRegistry;

    public TelegramCustomerServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public void accountViewed(String result) {
        meterRegistry.counter("telegram_account_views_total", "result", safe(result)).increment();
    }

    public void servicesViewed(String result) {
        meterRegistry.counter("telegram_services_views_total", "result", safe(result)).increment();
    }

    public void serviceDetailsViewed(String status, String freshness) {
        meterRegistry.counter(
                "telegram_service_details_views_total",
                "status", safe(status),
                "freshness", safe(freshness)
        ).increment();
    }

    public void serviceSearch(String result) {
        meterRegistry.counter("telegram_service_search_total", "result", safe(result)).increment();
    }

    public void serviceRefresh(String result, String freshness) {
        meterRegistry.counter(
                "telegram_service_refresh_total",
                "result", safe(result),
                "freshness", safe(freshness)
        ).increment();
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    }
}
