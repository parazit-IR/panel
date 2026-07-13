package com.parazit.panel.domain.order;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

public record RenewalSnapshot(
        UUID targetSubscriptionId,
        UUID targetProvisionId,
        String serviceDisplayName,
        String serviceUsername,
        Instant currentExpiryAt,
        Long currentTrafficLimitBytes,
        Long currentUsedTrafficBytes,
        Duration renewalDuration,
        Long renewalTrafficBytes,
        RenewalTrafficPolicy trafficPolicy,
        Money originalAmount,
        Money finalAmount,
        String planName,
        String planDescription,
        UUID sourcePlanId,
        Instant capturedAt
) {

    public RenewalSnapshot {
        targetSubscriptionId = Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        targetProvisionId = Objects.requireNonNull(targetProvisionId, "targetProvisionId must not be null");
        serviceDisplayName = requireText(serviceDisplayName, "serviceDisplayName", 200);
        serviceUsername = requireText(serviceUsername, "serviceUsername", 200);
        if (currentTrafficLimitBytes != null && currentTrafficLimitBytes < 0) {
            throw new IllegalArgumentException("currentTrafficLimitBytes must be zero or positive");
        }
        if (currentUsedTrafficBytes != null && currentUsedTrafficBytes < 0) {
            throw new IllegalArgumentException("currentUsedTrafficBytes must be zero or positive");
        }
        renewalDuration = requirePositiveDuration(renewalDuration);
        if (renewalTrafficBytes != null && renewalTrafficBytes <= 0) {
            throw new IllegalArgumentException("renewalTrafficBytes must be positive when present");
        }
        trafficPolicy = Objects.requireNonNull(trafficPolicy, "trafficPolicy must not be null");
        originalAmount = Objects.requireNonNull(originalAmount, "originalAmount must not be null");
        finalAmount = Objects.requireNonNull(finalAmount, "finalAmount must not be null");
        if (!originalAmount.currency().equals(finalAmount.currency())) {
            throw new IllegalArgumentException("originalAmount and finalAmount currencies must match");
        }
        planName = requireText(planName, "planName", 128);
        planDescription = normalizeOptional(planDescription, 1000);
        sourcePlanId = Objects.requireNonNull(sourcePlanId, "sourcePlanId must not be null");
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
    }

    public OptionalLong currentTrafficLimit() {
        return currentTrafficLimitBytes == null ? OptionalLong.empty() : OptionalLong.of(currentTrafficLimitBytes);
    }

    public OptionalLong currentUsedTraffic() {
        return currentUsedTrafficBytes == null ? OptionalLong.empty() : OptionalLong.of(currentUsedTrafficBytes);
    }

    public OptionalLong renewalTraffic() {
        return renewalTrafficBytes == null ? OptionalLong.empty() : OptionalLong.of(renewalTrafficBytes);
    }

    private static Duration requirePositiveDuration(Duration value) {
        Duration duration = Objects.requireNonNull(value, "renewalDuration must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("renewalDuration must be positive");
        }
        return duration;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
