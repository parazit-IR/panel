package com.parazit.panel.application.renewal.result;

import com.parazit.panel.domain.renewal.RenewalApplyOutcome;
import java.time.Instant;
import java.util.UUID;

public record ApplyRenewalResult(
        UUID renewalOrderId,
        UUID subscriptionId,
        RenewalApplyOutcome outcome,
        Instant previousExpiryAt,
        Instant newExpiryAt,
        Long previousTrafficLimitBytes,
        long newTrafficLimitBytes,
        long effectiveUsedTrafficBytes,
        boolean remoteAlreadyApplied,
        boolean localStateUpdated,
        Instant completedAt
) {
}
