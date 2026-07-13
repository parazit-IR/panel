package com.parazit.panel.application.renewal.result;

import com.parazit.panel.domain.order.Money;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.UUID;

public record RenewalSelectionResult(
        UUID renewalSelectionId,
        UUID purchaseSessionId,
        UUID targetSubscriptionId,
        String serviceDisplayName,
        String serviceUsername,
        String planName,
        Duration renewalDuration,
        OptionalLong renewalTrafficBytes,
        Money amount,
        Instant selectionExpiresAt
) {
}
