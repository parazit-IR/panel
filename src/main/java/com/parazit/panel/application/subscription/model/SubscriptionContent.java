package com.parazit.panel.application.subscription.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SubscriptionContent(
        String title,
        List<SubscriptionConfigEntry> entries,
        Instant expiresAt,
        Long uploadBytes,
        Long downloadBytes,
        Long totalBytes,
        Long remainingBytes,
        String supportUrl,
        String profileUpdateInterval
) {

    public SubscriptionContent {
        title = title == null || title.isBlank() ? "Subscription" : title.trim();
        entries = List.copyOf(Objects.requireNonNull(entries, "entries must not be null"));
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("subscription content must contain at least one entry");
        }
        uploadBytes = nonNegativeOrNull(uploadBytes, "uploadBytes");
        downloadBytes = nonNegativeOrNull(downloadBytes, "downloadBytes");
        totalBytes = nonNegativeOrNull(totalBytes, "totalBytes");
        remainingBytes = nonNegativeOrNull(remainingBytes, "remainingBytes");
    }

    private static Long nonNegativeOrNull(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }
}
