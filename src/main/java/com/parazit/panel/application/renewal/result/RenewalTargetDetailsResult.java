package com.parazit.panel.application.renewal.result;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public record RenewalTargetDetailsResult(
        UUID subscriptionId,
        String displayName,
        String serviceUsername,
        CustomerServiceStatus status,
        String currentPlanName,
        Optional<Instant> activatedAt,
        Optional<Instant> currentExpiryAt,
        Optional<Duration> remainingDuration,
        OptionalLong currentTrafficLimitBytes,
        OptionalLong usedTrafficBytes,
        OptionalLong remainingTrafficBytes,
        boolean renewable,
        Optional<String> unavailableMessageKey
) {
}
