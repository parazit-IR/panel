package com.parazit.panel.application.renewal.result;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.application.renewal.RenewalIneligibilityReason;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public record RenewableServiceSummaryResult(
        UUID subscriptionId,
        String displayName,
        String serviceUsername,
        CustomerServiceStatus status,
        Optional<Instant> expiresAt,
        Optional<Duration> remainingDuration,
        OptionalLong totalTrafficBytes,
        OptionalLong remainingTrafficBytes,
        boolean renewable,
        Optional<RenewalIneligibilityReason> ineligibilityReason
) {
}
