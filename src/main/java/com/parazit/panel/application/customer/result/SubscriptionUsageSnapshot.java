package com.parazit.panel.application.customer.result;

import java.time.Instant;
import java.util.OptionalLong;

public record SubscriptionUsageSnapshot(
        OptionalLong totalBytes,
        OptionalLong usedBytes,
        OptionalLong remainingBytes,
        Instant fetchedAt,
        UsageFreshness freshness
) {

    public SubscriptionUsageSnapshot {
        if (freshness == null) {
            throw new IllegalArgumentException("freshness must not be null");
        }
        totalBytes = totalBytes == null ? OptionalLong.empty() : totalBytes;
        usedBytes = usedBytes == null ? OptionalLong.empty() : usedBytes;
        remainingBytes = remainingBytes == null ? OptionalLong.empty() : remainingBytes;
        if (freshness != UsageFreshness.UNAVAILABLE && fetchedAt == null) {
            throw new IllegalArgumentException("fetchedAt is required for available usage");
        }
        validateNonNegative(totalBytes, "totalBytes");
        validateNonNegative(usedBytes, "usedBytes");
        validateNonNegative(remainingBytes, "remainingBytes");
    }

    public static SubscriptionUsageSnapshot unavailable() {
        return new SubscriptionUsageSnapshot(OptionalLong.empty(), OptionalLong.empty(), OptionalLong.empty(), null, UsageFreshness.UNAVAILABLE);
    }

    public static SubscriptionUsageSnapshot of(long totalBytes, long usedBytes, Instant fetchedAt, UsageFreshness freshness) {
        if (totalBytes < 0 || usedBytes < 0) {
            throw new IllegalArgumentException("usage values must be non-negative");
        }
        long remaining = Math.max(totalBytes - usedBytes, 0L);
        return new SubscriptionUsageSnapshot(OptionalLong.of(totalBytes), OptionalLong.of(usedBytes), OptionalLong.of(remaining), fetchedAt, freshness);
    }

    private static void validateNonNegative(OptionalLong value, String field) {
        if (value.isPresent() && value.getAsLong() < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
    }
}
