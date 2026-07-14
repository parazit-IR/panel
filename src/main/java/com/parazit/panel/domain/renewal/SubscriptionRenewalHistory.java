package com.parazit.panel.domain.renewal;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "subscription_renewal_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subscription_renewal_history_order", columnNames = "renewal_order_id")
        },
        indexes = {
                @Index(name = "idx_subscription_renewal_history_subscription_applied", columnList = "subscription_id,applied_at")
        }
)
public class SubscriptionRenewalHistory extends BaseEntity {

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "renewal_order_id", nullable = false, updatable = false)
    private UUID renewalOrderId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "previous_expiry_at", updatable = false)
    private Instant previousExpiryAt;

    @Column(name = "new_expiry_at", nullable = false, updatable = false)
    private Instant newExpiryAt;

    @Column(name = "previous_traffic_limit_bytes", updatable = false)
    private Long previousTrafficLimitBytes;

    @Column(name = "new_traffic_limit_bytes", nullable = false, updatable = false)
    private long newTrafficLimitBytes;

    @Column(name = "previous_used_traffic_bytes", updatable = false)
    private Long previousUsedTrafficBytes;

    @Column(name = "new_used_traffic_bytes", nullable = false, updatable = false)
    private long newUsedTrafficBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_policy", nullable = false, length = 40, updatable = false)
    private RenewalTrafficPolicy trafficPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "expiry_policy", nullable = false, length = 60, updatable = false)
    private RenewalExpiryPolicy expiryPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32, updatable = false)
    private RenewalResultStatus status;

    @Column(name = "provider_request_id", length = 128, updatable = false)
    private String providerRequestId;

    @Column(name = "provider_status_code", length = 64, updatable = false)
    private String providerStatusCode;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SubscriptionRenewalHistory() {
    }

    private SubscriptionRenewalHistory(
            UUID subscriptionId,
            UUID renewalOrderId,
            UUID paymentId,
            UUID planId,
            Instant previousExpiryAt,
            Instant newExpiryAt,
            Long previousTrafficLimitBytes,
            long newTrafficLimitBytes,
            Long previousUsedTrafficBytes,
            long newUsedTrafficBytes,
            RenewalTrafficPolicy trafficPolicy,
            RenewalExpiryPolicy expiryPolicy,
            String providerRequestId,
            String providerStatusCode,
            Instant appliedAt
    ) {
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        this.renewalOrderId = Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.planId = Objects.requireNonNull(planId, "planId must not be null");
        this.previousExpiryAt = previousExpiryAt;
        this.newExpiryAt = Objects.requireNonNull(newExpiryAt, "newExpiryAt must not be null");
        this.previousTrafficLimitBytes = nonNegative(previousTrafficLimitBytes, "previousTrafficLimitBytes");
        this.newTrafficLimitBytes = nonNegative(newTrafficLimitBytes, "newTrafficLimitBytes");
        this.previousUsedTrafficBytes = nonNegative(previousUsedTrafficBytes, "previousUsedTrafficBytes");
        this.newUsedTrafficBytes = nonNegative(newUsedTrafficBytes, "newUsedTrafficBytes");
        this.trafficPolicy = Objects.requireNonNull(trafficPolicy, "trafficPolicy must not be null");
        this.expiryPolicy = Objects.requireNonNull(expiryPolicy, "expiryPolicy must not be null");
        this.status = RenewalResultStatus.APPLIED;
        this.providerRequestId = normalizeOptional(providerRequestId, 128);
        this.providerStatusCode = normalizeOptional(providerStatusCode, 64);
        this.appliedAt = Objects.requireNonNull(appliedAt, "appliedAt must not be null");
    }

    public static SubscriptionRenewalHistory applied(
            UUID subscriptionId,
            UUID renewalOrderId,
            UUID paymentId,
            UUID planId,
            Instant previousExpiryAt,
            Instant newExpiryAt,
            Long previousTrafficLimitBytes,
            long newTrafficLimitBytes,
            Long previousUsedTrafficBytes,
            long newUsedTrafficBytes,
            RenewalTrafficPolicy trafficPolicy,
            RenewalExpiryPolicy expiryPolicy,
            String providerRequestId,
            String providerStatusCode,
            Instant appliedAt
    ) {
        return new SubscriptionRenewalHistory(
                subscriptionId,
                renewalOrderId,
                paymentId,
                planId,
                previousExpiryAt,
                newExpiryAt,
                previousTrafficLimitBytes,
                newTrafficLimitBytes,
                previousUsedTrafficBytes,
                newUsedTrafficBytes,
                trafficPolicy,
                expiryPolicy,
                providerRequestId,
                providerStatusCode,
                appliedAt
        );
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getRenewalOrderId() {
        return renewalOrderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public Instant getNewExpiryAt() {
        return newExpiryAt;
    }

    public long getNewTrafficLimitBytes() {
        return newTrafficLimitBytes;
    }

    public long getNewUsedTrafficBytes() {
        return newUsedTrafficBytes;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    private static long nonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    private static Long nonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
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
