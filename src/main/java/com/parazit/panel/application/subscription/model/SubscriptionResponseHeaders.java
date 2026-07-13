package com.parazit.panel.application.subscription.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record SubscriptionResponseHeaders(Map<String, String> values) {

    public SubscriptionResponseHeaders {
        Map<String, String> sanitized = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((name, value) -> {
                if (name != null && value != null && !name.isBlank() && !value.isBlank()) {
                    sanitized.put(sanitize(name), sanitize(value));
                }
            });
        }
        values = Collections.unmodifiableMap(sanitized);
    }

    private static String sanitize(String value) {
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
