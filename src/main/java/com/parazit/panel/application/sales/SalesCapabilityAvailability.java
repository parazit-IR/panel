package com.parazit.panel.application.sales;

import java.time.Instant;
import java.util.Objects;

public record SalesCapabilityAvailability(
        SalesCapability capability,
        boolean visible,
        boolean enabled,
        String unavailableReasonCode,
        String unavailableMessageKey,
        Instant evaluatedAt
) {

    public SalesCapabilityAvailability {
        Objects.requireNonNull(capability, "capability must not be null");
        unavailableReasonCode = normalize(unavailableReasonCode);
        unavailableMessageKey = normalize(unavailableMessageKey);
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        if (!visible && enabled) {
            throw new IllegalArgumentException("hidden capability cannot be enabled");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }
}
