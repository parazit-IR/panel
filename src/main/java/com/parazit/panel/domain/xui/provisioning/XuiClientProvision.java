package com.parazit.panel.domain.xui.provisioning;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "xui_client_provisions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_xui_client_provisions_plan_selection", columnNames = "plan_selection_id"),
                @UniqueConstraint(name = "uq_xui_client_provisions_remote_client", columnNames = "remote_client_id"),
                @UniqueConstraint(name = "uq_xui_client_provisions_remote_email", columnNames = "remote_email")
        },
        indexes = {
                @Index(name = "idx_xui_client_provisions_user_id", columnList = "user_id"),
                @Index(name = "idx_xui_client_provisions_plan_id", columnList = "plan_id"),
                @Index(name = "idx_xui_client_provisions_plan_selection_id", columnList = "plan_selection_id"),
                @Index(name = "idx_xui_client_provisions_inbound_id", columnList = "inbound_id"),
                @Index(name = "idx_xui_client_provisions_status", columnList = "status"),
                @Index(name = "idx_xui_client_provisions_expires_at", columnList = "expires_at"),
                @Index(name = "idx_xui_client_provisions_remote_client_id", columnList = "remote_client_id"),
                @Index(name = "idx_xui_client_provisions_remote_email", columnList = "remote_email")
        }
)
public class XuiClientProvision extends BaseEntity {

    public static final int REMOTE_CLIENT_ID_MAX_LENGTH = 64;
    public static final int REMOTE_EMAIL_MAX_LENGTH = 128;
    public static final int REMOTE_SUBSCRIPTION_ID_MAX_LENGTH = 128;
    public static final int FAILURE_CODE_MAX_LENGTH = 64;
    public static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "plan_selection_id", nullable = false, updatable = false)
    private UUID planSelectionId;

    @Column(name = "inbound_id", nullable = false, updatable = false)
    private long inboundId;

    @Column(name = "remote_client_id", nullable = false, length = REMOTE_CLIENT_ID_MAX_LENGTH, updatable = false)
    private String remoteClientId;

    @Column(name = "remote_email", nullable = false, length = REMOTE_EMAIL_MAX_LENGTH, updatable = false)
    private String remoteEmail;

    @Column(name = "remote_subscription_id", length = REMOTE_SUBSCRIPTION_ID_MAX_LENGTH, updatable = false)
    private String remoteSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private XuiProvisionStatus status;

    @Column(name = "traffic_limit_bytes", nullable = false, updatable = false)
    private long trafficLimitBytes;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "ip_limit", nullable = false, updatable = false)
    private int ipLimit;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_synchronized_at")
    private Instant lastSynchronizedAt;

    @Column(name = "failure_code", length = FAILURE_CODE_MAX_LENGTH)
    private String failureCode;

    @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
    private String failureMessage;

    protected XuiClientProvision() {
    }

    private XuiClientProvision(
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            long inboundId,
            String remoteClientId,
            String remoteEmail,
            String remoteSubscriptionId,
            long trafficLimitBytes,
            Instant expiresAt,
            int ipLimit,
            Instant now
    ) {
        this.userId = requireUuid(userId, "userId");
        this.planId = requireUuid(planId, "planId");
        this.planSelectionId = requireUuid(planSelectionId, "planSelectionId");
        this.inboundId = requirePositive(inboundId, "inboundId");
        this.remoteClientId = requireClientUuid(remoteClientId);
        this.remoteEmail = requireText(remoteEmail, "remoteEmail", REMOTE_EMAIL_MAX_LENGTH);
        this.remoteSubscriptionId = normalizeOptional(remoteSubscriptionId, "remoteSubscriptionId", REMOTE_SUBSCRIPTION_ID_MAX_LENGTH);
        this.trafficLimitBytes = requireNonNegative(trafficLimitBytes, "trafficLimitBytes");
        this.expiresAt = requireFuture(expiresAt, now);
        this.ipLimit = requireNonNegativeInt(ipLimit, "ipLimit");
        this.status = XuiProvisionStatus.PENDING;
    }

    public static XuiClientProvision createPending(
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            long inboundId,
            String remoteClientId,
            String remoteEmail,
            String remoteSubscriptionId,
            long trafficLimitBytes,
            Instant expiresAt,
            int ipLimit,
            Instant now
    ) {
        return new XuiClientProvision(
                userId,
                planId,
                planSelectionId,
                inboundId,
                remoteClientId,
                remoteEmail,
                remoteSubscriptionId,
                trafficLimitBytes,
                expiresAt,
                ipLimit,
                Objects.requireNonNull(now, "now must not be null")
        );
    }

    public void markProvisioning() {
        if (status == XuiProvisionStatus.PROVISIONING) {
            return;
        }
        if (status == XuiProvisionStatus.ACTIVE) {
            return;
        }
        if (status != XuiProvisionStatus.PENDING
                && status != XuiProvisionStatus.FAILED
                && status != XuiProvisionStatus.UNKNOWN) {
            throw new IllegalStateException("cannot mark provisioning from status " + status);
        }
        status = XuiProvisionStatus.PROVISIONING;
        clearFailure();
    }

    public void markActive(Instant provisionedAt) {
        Instant requiredProvisionedAt = Objects.requireNonNull(provisionedAt, "provisionedAt must not be null");
        if (status == XuiProvisionStatus.ACTIVE) {
            return;
        }
        if (status != XuiProvisionStatus.PROVISIONING && status != XuiProvisionStatus.UNKNOWN) {
            throw new IllegalStateException("cannot mark active from status " + status);
        }
        this.status = XuiProvisionStatus.ACTIVE;
        this.provisionedAt = requiredProvisionedAt;
        clearFailure();
    }

    public void markFailed(String failureCode, String safeMessage) {
        if (status == XuiProvisionStatus.ACTIVE
                || status == XuiProvisionStatus.DISABLED
                || status == XuiProvisionStatus.DELETED) {
            throw new IllegalStateException("cannot fail a confirmed provision with status " + status);
        }
        status = XuiProvisionStatus.FAILED;
        this.failureCode = normalizeOptional(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = normalizeFailureMessage(safeMessage);
    }

    public void markUnknown(String failureCode, String safeMessage) {
        if (status == XuiProvisionStatus.ACTIVE
                || status == XuiProvisionStatus.DISABLED
                || status == XuiProvisionStatus.DELETED) {
            return;
        }
        status = XuiProvisionStatus.UNKNOWN;
        this.failureCode = normalizeOptional(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = normalizeFailureMessage(safeMessage);
    }

    public void markDisabling() {
        if (status == XuiProvisionStatus.DISABLING
                || status == XuiProvisionStatus.DISABLED
                || status == XuiProvisionStatus.DELETED) {
            return;
        }
        if (status != XuiProvisionStatus.ACTIVE
                && status != XuiProvisionStatus.UNKNOWN
                && status != XuiProvisionStatus.FAILED) {
            throw new IllegalStateException("cannot mark disabling from status " + status);
        }
        status = XuiProvisionStatus.DISABLING;
        clearFailure();
    }

    public void markDisabled(Instant disabledAt) {
        Instant requiredDisabledAt = Objects.requireNonNull(disabledAt, "disabledAt must not be null");
        if (status == XuiProvisionStatus.DISABLED) {
            return;
        }
        if (status != XuiProvisionStatus.DISABLING && status != XuiProvisionStatus.UNKNOWN) {
            throw new IllegalStateException("cannot mark disabled from status " + status);
        }
        status = XuiProvisionStatus.DISABLED;
        this.disabledAt = requiredDisabledAt;
        clearFailure();
    }

    public void markDeleting() {
        if (status == XuiProvisionStatus.DELETING || status == XuiProvisionStatus.DELETED) {
            return;
        }
        if (status != XuiProvisionStatus.ACTIVE
                && status != XuiProvisionStatus.DISABLED
                && status != XuiProvisionStatus.UNKNOWN
                && status != XuiProvisionStatus.FAILED) {
            throw new IllegalStateException("cannot mark deleting from status " + status);
        }
        status = XuiProvisionStatus.DELETING;
        clearFailure();
    }

    public void markDeleted(Instant deletedAt) {
        Instant requiredDeletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
        if (status == XuiProvisionStatus.DELETED) {
            return;
        }
        if (status != XuiProvisionStatus.DELETING && status != XuiProvisionStatus.UNKNOWN) {
            throw new IllegalStateException("cannot mark deleted from status " + status);
        }
        status = XuiProvisionStatus.DELETED;
        this.deletedAt = requiredDeletedAt;
        clearFailure();
    }

    public void markOperationUnknown(String failureCode, String safeMessage) {
        if (status == XuiProvisionStatus.DELETED) {
            return;
        }
        status = XuiProvisionStatus.UNKNOWN;
        this.failureCode = normalizeOptional(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = normalizeFailureMessage(safeMessage);
    }

    public void markOperationFailed(String failureCode, String safeMessage) {
        if (status == XuiProvisionStatus.DELETED) {
            throw new IllegalStateException("cannot fail a deleted provision");
        }
        status = XuiProvisionStatus.FAILED;
        this.failureCode = normalizeOptional(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = normalizeFailureMessage(safeMessage);
    }

    public void markSynchronized(Instant synchronizedAt) {
        this.lastSynchronizedAt = Objects.requireNonNull(synchronizedAt, "synchronizedAt must not be null");
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

    public long getInboundId() {
        return inboundId;
    }

    public String getRemoteClientId() {
        return remoteClientId;
    }

    public String getRemoteEmail() {
        return remoteEmail;
    }

    public String getRemoteSubscriptionId() {
        return remoteSubscriptionId;
    }

    public XuiProvisionStatus getStatus() {
        return status;
    }

    public long getTrafficLimitBytes() {
        return trafficLimitBytes;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getIpLimit() {
        return ipLimit;
    }

    public Instant getProvisionedAt() {
        return provisionedAt;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getLastSynchronizedAt() {
        return lastSynchronizedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    private static UUID requireUuid(UUID value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " must not be null");
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static long requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    private static int requireNonNegativeInt(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    private static String requireClientUuid(String value) {
        String normalized = requireText(value, "remoteClientId", REMOTE_CLIENT_ID_MAX_LENGTH);
        UUID.fromString(normalized);
        return normalized;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fieldName, int maxLength) {
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

    private static Instant requireFuture(Instant expiresAt, Instant now) {
        Instant requiredExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!requiredExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
        return requiredExpiresAt;
    }

    private static String normalizeFailureMessage(String safeMessage) {
        return normalizeOptional(safeMessage, "failureMessage", FAILURE_MESSAGE_MAX_LENGTH);
    }

    private void clearFailure() {
        failureCode = null;
        failureMessage = null;
    }
}
