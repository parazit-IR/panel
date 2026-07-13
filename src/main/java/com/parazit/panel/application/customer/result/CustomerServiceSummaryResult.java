package com.parazit.panel.application.customer.result;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public record CustomerServiceSummaryResult(
        UUID subscriptionId,
        String displayName,
        String serviceUsername,
        CustomerServiceStatus status,
        String planName,
        OptionalLong totalTrafficBytes,
        OptionalLong usedTrafficBytes,
        OptionalLong remainingTrafficBytes,
        Optional<Instant> usageUpdatedAt,
        Optional<Instant> expiresAt,
        Optional<Duration> remainingDuration,
        boolean subscriptionContentAvailable,
        boolean qrAvailable,
        boolean vlessAvailable,
        boolean renewalAvailable
) {

    public CustomerServiceSummaryResult {
        if (subscriptionId == null) {
            throw new IllegalArgumentException("subscriptionId must not be null");
        }
        displayName = displayName == null || displayName.isBlank() ? "Service" : displayName.trim();
        serviceUsername = serviceUsername == null || serviceUsername.isBlank() ? displayName : serviceUsername.trim();
        status = status == null ? CustomerServiceStatus.UNKNOWN : status;
        planName = planName == null || planName.isBlank() ? "Plan" : planName.trim();
        totalTrafficBytes = totalTrafficBytes == null ? OptionalLong.empty() : totalTrafficBytes;
        usedTrafficBytes = usedTrafficBytes == null ? OptionalLong.empty() : usedTrafficBytes;
        remainingTrafficBytes = remainingTrafficBytes == null ? OptionalLong.empty() : remainingTrafficBytes;
        usageUpdatedAt = usageUpdatedAt == null ? Optional.empty() : usageUpdatedAt;
        expiresAt = expiresAt == null ? Optional.empty() : expiresAt;
        remainingDuration = remainingDuration == null ? Optional.empty() : remainingDuration;
    }
}
