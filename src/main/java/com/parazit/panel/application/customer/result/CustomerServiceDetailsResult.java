package com.parazit.panel.application.customer.result;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public record CustomerServiceDetailsResult(
        UUID subscriptionId,
        String displayName,
        String serviceUsername,
        CustomerServiceStatus status,
        String planName,
        Duration planDuration,
        OptionalLong totalTrafficBytes,
        OptionalLong usedTrafficBytes,
        OptionalLong remainingTrafficBytes,
        Optional<Instant> usageUpdatedAt,
        UsageFreshness usageFreshness,
        Optional<Instant> activatedAt,
        Optional<Instant> expiresAt,
        Optional<Duration> remainingDuration,
        boolean contentAvailable,
        boolean qrAvailable,
        boolean vlessAvailable,
        boolean renewalAvailable,
        boolean refreshAvailable
) {

    public CustomerServiceDetailsResult {
        if (subscriptionId == null) {
            throw new IllegalArgumentException("subscriptionId must not be null");
        }
        displayName = displayName == null || displayName.isBlank() ? "Service" : displayName.trim();
        serviceUsername = serviceUsername == null || serviceUsername.isBlank() ? displayName : serviceUsername.trim();
        status = status == null ? CustomerServiceStatus.UNKNOWN : status;
        planName = planName == null || planName.isBlank() ? "Plan" : planName.trim();
        planDuration = planDuration == null ? Duration.ZERO : planDuration;
        totalTrafficBytes = totalTrafficBytes == null ? OptionalLong.empty() : totalTrafficBytes;
        usedTrafficBytes = usedTrafficBytes == null ? OptionalLong.empty() : usedTrafficBytes;
        remainingTrafficBytes = remainingTrafficBytes == null ? OptionalLong.empty() : remainingTrafficBytes;
        usageUpdatedAt = usageUpdatedAt == null ? Optional.empty() : usageUpdatedAt;
        usageFreshness = usageFreshness == null ? UsageFreshness.UNAVAILABLE : usageFreshness;
        activatedAt = activatedAt == null ? Optional.empty() : activatedAt;
        expiresAt = expiresAt == null ? Optional.empty() : expiresAt;
        remainingDuration = remainingDuration == null ? Optional.empty() : remainingDuration;
    }
}
