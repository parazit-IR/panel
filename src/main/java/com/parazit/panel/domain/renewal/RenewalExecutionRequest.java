package com.parazit.panel.domain.renewal;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RenewalExecutionRequest(
        UUID renewalOrderId,
        UUID paymentId,
        UUID userId,
        UUID targetSubscriptionId,
        UUID targetProvisionId,
        UUID sourcePlanId,
        String serviceUsername,
        RenewalTrafficPolicy trafficPolicy,
        RenewalExpiryPolicy expiryPolicy,
        Instant previousExpiryAt,
        Instant proposedExpiryAt,
        Long previousTrafficLimitBytes,
        Long previousUsedTrafficBytes,
        Long renewalTrafficBytes,
        Money paidAmount,
        CurrencyCode currency,
        Instant paymentApprovedAt,
        Instant requestedAt,
        String snapshotVersion
) {

    public RenewalExecutionRequest {
        renewalOrderId = Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
        paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        userId = Objects.requireNonNull(userId, "userId must not be null");
        targetSubscriptionId = Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        targetProvisionId = Objects.requireNonNull(targetProvisionId, "targetProvisionId must not be null");
        sourcePlanId = Objects.requireNonNull(sourcePlanId, "sourcePlanId must not be null");
        serviceUsername = requireText(serviceUsername, "serviceUsername", 200);
        trafficPolicy = Objects.requireNonNull(trafficPolicy, "trafficPolicy must not be null");
        expiryPolicy = Objects.requireNonNull(expiryPolicy, "expiryPolicy must not be null");
        proposedExpiryAt = Objects.requireNonNull(proposedExpiryAt, "proposedExpiryAt must not be null");
        previousTrafficLimitBytes = requireOptionalNonNegative(previousTrafficLimitBytes, "previousTrafficLimitBytes");
        previousUsedTrafficBytes = requireOptionalNonNegative(previousUsedTrafficBytes, "previousUsedTrafficBytes");
        renewalTrafficBytes = requireOptionalPositive(renewalTrafficBytes, "renewalTrafficBytes");
        paidAmount = Objects.requireNonNull(paidAmount, "paidAmount must not be null");
        currency = Objects.requireNonNull(currency, "currency must not be null");
        if (!paidAmount.currency().equals(currency)) {
            throw new IllegalArgumentException("paidAmount currency must match currency");
        }
        paymentApprovedAt = Objects.requireNonNull(paymentApprovedAt, "paymentApprovedAt must not be null");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        snapshotVersion = requireText(snapshotVersion, "snapshotVersion", 64);
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static Long requireOptionalNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    private static Long requireOptionalPositive(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
