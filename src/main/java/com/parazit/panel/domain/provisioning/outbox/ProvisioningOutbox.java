package com.parazit.panel.domain.provisioning.outbox;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "provisioning_outbox",
        indexes = {
                @Index(name = "idx_provisioning_outbox_status_available", columnList = "status,available_at"),
                @Index(name = "idx_provisioning_outbox_order_id", columnList = "order_id"),
                @Index(name = "idx_provisioning_outbox_payment_id", columnList = "payment_id"),
                @Index(name = "idx_provisioning_outbox_user_id", columnList = "user_id"),
                @Index(name = "idx_provisioning_outbox_plan_selection_id", columnList = "plan_selection_id")
        }
)
public class ProvisioningOutbox extends BaseEntity {

    public static final int PAYLOAD_VERSION_MAX_LENGTH = 64;
    public static final int FAILURE_CODE_MAX_LENGTH = 64;
    public static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "plan_selection_id", nullable = false, updatable = false)
    private UUID planSelectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40, updatable = false)
    private ProvisioningOutboxType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProvisioningOutboxStatus status;

    @Column(name = "payload_version", nullable = false, length = PAYLOAD_VERSION_MAX_LENGTH, updatable = false)
    private String payloadVersion;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb", updatable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    @Column(name = "failure_code", length = FAILURE_CODE_MAX_LENGTH)
    private String failureCode;

    @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
    private String failureMessage;

    protected ProvisioningOutbox() {
    }

    private ProvisioningOutbox(
            UUID eventId,
            UUID orderId,
            UUID paymentId,
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            ProvisioningOutboxType type,
            String payloadVersion,
            String payload,
            Instant availableAt
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.planId = Objects.requireNonNull(planId, "planId must not be null");
        this.planSelectionId = Objects.requireNonNull(planSelectionId, "planSelectionId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payloadVersion = requireText(payloadVersion, "payloadVersion", PAYLOAD_VERSION_MAX_LENGTH);
        this.payload = requireText(payload, "payload", 20_000);
        this.availableAt = Objects.requireNonNull(availableAt, "availableAt must not be null");
        this.status = ProvisioningOutboxStatus.PENDING;
    }

    public static ProvisioningOutbox create(
            UUID eventId,
            UUID orderId,
            UUID paymentId,
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            ProvisioningOutboxType type,
            String payloadVersion,
            String payload,
            Instant availableAt
    ) {
        return new ProvisioningOutbox(
                eventId,
                orderId,
                paymentId,
                userId,
                planId,
                planSelectionId,
                type,
                payloadVersion,
                payload,
                availableAt
        );
    }

    public void markProcessing(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == ProvisioningOutboxStatus.PROCESSING) {
            return;
        }
        if (status != ProvisioningOutboxStatus.PENDING
                && status != ProvisioningOutboxStatus.FAILED
                && status != ProvisioningOutboxStatus.UNKNOWN) {
            throw invalidTransition("process");
        }
        status = ProvisioningOutboxStatus.PROCESSING;
        processingStartedAt = now;
    }

    public void markProcessed(Instant now) {
        if (status == ProvisioningOutboxStatus.PROCESSED) {
            return;
        }
        requireStatus(ProvisioningOutboxStatus.PROCESSING, "mark processed");
        status = ProvisioningOutboxStatus.PROCESSED;
        processedAt = Objects.requireNonNull(now, "now must not be null");
        failureCode = null;
        failureMessage = null;
    }

    public void markFailed(String code, String message, Instant failedAt, Instant nextAvailableAt) {
        requireStatus(ProvisioningOutboxStatus.PROCESSING, "mark failed");
        status = ProvisioningOutboxStatus.FAILED;
        attemptCount++;
        lastFailedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
        availableAt = Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");
        failureCode = normalizeOptional(code, FAILURE_CODE_MAX_LENGTH);
        failureMessage = normalizeOptional(message, FAILURE_MESSAGE_MAX_LENGTH);
    }

    public void markUnknown(String code, String message, Instant failedAt, Instant nextAvailableAt) {
        requireStatus(ProvisioningOutboxStatus.PROCESSING, "mark unknown");
        status = ProvisioningOutboxStatus.UNKNOWN;
        attemptCount++;
        lastFailedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
        availableAt = Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");
        failureCode = normalizeOptional(code, FAILURE_CODE_MAX_LENGTH);
        failureMessage = normalizeOptional(message, FAILURE_MESSAGE_MAX_LENGTH);
    }

    public void markDead(String code, String message, Instant failedAt) {
        if (status == ProvisioningOutboxStatus.DEAD) {
            return;
        }
        if (status == ProvisioningOutboxStatus.PROCESSED) {
            throw invalidTransition("mark dead");
        }
        status = ProvisioningOutboxStatus.DEAD;
        lastFailedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
        failureCode = normalizeOptional(code, FAILURE_CODE_MAX_LENGTH);
        failureMessage = normalizeOptional(message, FAILURE_MESSAGE_MAX_LENGTH);
    }

    public void retryNow(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status != ProvisioningOutboxStatus.FAILED
                && status != ProvisioningOutboxStatus.UNKNOWN
                && status != ProvisioningOutboxStatus.DEAD) {
            throw invalidTransition("retry");
        }
        status = ProvisioningOutboxStatus.PENDING;
        availableAt = now;
        processingStartedAt = null;
    }

    public boolean isAvailable(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return (status == ProvisioningOutboxStatus.PENDING
                || status == ProvisioningOutboxStatus.FAILED
                || status == ProvisioningOutboxStatus.UNKNOWN)
                && !availableAt.isAfter(now);
    }

    public boolean isProcessingStale(Instant now, java.time.Duration timeout) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");
        return status == ProvisioningOutboxStatus.PROCESSING
                && processingStartedAt != null
                && !processingStartedAt.plus(timeout).isAfter(now);
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public UUID getPlanSelectionId() {
        return planSelectionId;
    }

    public ProvisioningOutboxType getType() {
        return type;
    }

    public ProvisioningOutboxStatus getStatus() {
        return status;
    }

    public String getPayloadVersion() {
        return payloadVersion;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getLastFailedAt() {
        return lastFailedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    private void requireStatus(ProvisioningOutboxStatus expected, String operation) {
        if (status != expected) {
            throw invalidTransition(operation);
        }
    }

    private IllegalStateException invalidTransition(String operation) {
        return new IllegalStateException("cannot " + operation + " provisioning outbox with status " + status);
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

    private static String normalizeOptional(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }
}
