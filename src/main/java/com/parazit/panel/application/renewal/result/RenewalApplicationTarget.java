package com.parazit.panel.application.renewal.result;

import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RenewalApplicationTarget(
        UUID renewalOrderId,
        UUID targetSubscriptionId,
        UUID targetProvisionId,
        Instant desiredExpiryAt,
        long desiredTotalTrafficBytes,
        boolean resetUsage,
        RenewalTrafficPolicy trafficPolicy,
        Instant calculatedAt,
        String calculationVersion
) {

    public static final String VERSION_V1 = "renewal-application-target.v1";

    public RenewalApplicationTarget {
        renewalOrderId = Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
        targetSubscriptionId = Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        targetProvisionId = Objects.requireNonNull(targetProvisionId, "targetProvisionId must not be null");
        desiredExpiryAt = Objects.requireNonNull(desiredExpiryAt, "desiredExpiryAt must not be null");
        if (desiredTotalTrafficBytes < 0) {
            throw new IllegalArgumentException("desiredTotalTrafficBytes must be zero or positive");
        }
        trafficPolicy = Objects.requireNonNull(trafficPolicy, "trafficPolicy must not be null");
        calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        calculationVersion = Objects.requireNonNull(calculationVersion, "calculationVersion must not be null").trim();
        if (calculationVersion.isBlank()) {
            throw new IllegalArgumentException("calculationVersion must not be blank");
        }
    }
}
