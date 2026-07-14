package com.parazit.panel.domain.renewal;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "renewal_outbox",
        indexes = {
                @Index(name = "idx_renewal_outbox_status_available", columnList = "status,available_at"),
                @Index(name = "idx_renewal_outbox_payment_id", columnList = "payment_id"),
                @Index(name = "idx_renewal_outbox_target_subscription", columnList = "target_subscription_id")
        }
)
public class RenewalOutbox extends BaseEntity {

    public static final String APPLY_REQUESTED_EVENT_TYPE = "renewal.apply.requested.v1";
    public static final String PAYLOAD_VERSION_V1 = "renewal-execution-request.v1";
    public static final int EVENT_TYPE_MAX_LENGTH = 80;
    public static final int PAYLOAD_VERSION_MAX_LENGTH = 64;
    public static final int ERROR_CODE_MAX_LENGTH = 64;

    @Column(name = "renewal_order_id", nullable = false, updatable = false)
    private UUID renewalOrderId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "target_subscription_id", nullable = false, updatable = false)
    private UUID targetSubscriptionId;

    @Column(name = "target_provision_id", nullable = false, updatable = false)
    private UUID targetProvisionId;

    @Column(name = "event_type", nullable = false, length = EVENT_TYPE_MAX_LENGTH, updatable = false)
    private String eventType;

    @Column(name = "payload_version", nullable = false, length = PAYLOAD_VERSION_MAX_LENGTH, updatable = false)
    private String payloadVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RenewalOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 128)
    private String lockedBy;

    @Column(name = "last_error_code", length = ERROR_CODE_MAX_LENGTH)
    private String lastErrorCode;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RenewalOutbox() {
    }

    private RenewalOutbox(
            UUID renewalOrderId,
            UUID paymentId,
            UUID targetSubscriptionId,
            UUID targetProvisionId,
            String eventType,
            String payloadVersion,
            String payload,
            Instant availableAt
    ) {
        this.renewalOrderId = Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.targetSubscriptionId = Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        this.targetProvisionId = Objects.requireNonNull(targetProvisionId, "targetProvisionId must not be null");
        this.eventType = requireText(eventType, "eventType", EVENT_TYPE_MAX_LENGTH);
        this.payloadVersion = requireText(payloadVersion, "payloadVersion", PAYLOAD_VERSION_MAX_LENGTH);
        this.payload = requireText(payload, "payload", 20_000);
        this.availableAt = Objects.requireNonNull(availableAt, "availableAt must not be null");
        this.status = RenewalOutboxStatus.PENDING;
    }

    public static RenewalOutbox create(
            UUID renewalOrderId,
            UUID paymentId,
            UUID targetSubscriptionId,
            UUID targetProvisionId,
            String payload,
            Instant availableAt
    ) {
        return new RenewalOutbox(
                renewalOrderId,
                paymentId,
                targetSubscriptionId,
                targetProvisionId,
                APPLY_REQUESTED_EVENT_TYPE,
                PAYLOAD_VERSION_V1,
                payload,
                availableAt
        );
    }

    public UUID getRenewalOrderId() {
        return renewalOrderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getTargetSubscriptionId() {
        return targetSubscriptionId;
    }

    public UUID getTargetProvisionId() {
        return targetProvisionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadVersion() {
        return payloadVersion;
    }

    public String getPayload() {
        return payload;
    }

    public RenewalOutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "RenewalOutbox[id=" + getId()
                + ", renewalOrderId=" + renewalOrderId
                + ", paymentId=" + paymentId
                + ", targetSubscriptionId=" + targetSubscriptionId
                + ", status=" + status
                + ", eventType=" + eventType
                + "]";
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
}
