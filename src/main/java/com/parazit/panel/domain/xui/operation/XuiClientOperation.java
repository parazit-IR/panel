package com.parazit.panel.domain.xui.operation;

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
        name = "xui_client_operations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_xui_client_operations_operation_id", columnNames = "operation_id")
        },
        indexes = {
                @Index(name = "idx_xui_client_operations_provision_id", columnList = "provision_id"),
                @Index(name = "idx_xui_client_operations_operation_id", columnList = "operation_id"),
                @Index(name = "idx_xui_client_operations_type", columnList = "type"),
                @Index(name = "idx_xui_client_operations_status", columnList = "status"),
                @Index(name = "idx_xui_client_operations_requested_at", columnList = "requested_at")
        }
)
public class XuiClientOperation extends BaseEntity {

    public static final int FINGERPRINT_MAX_LENGTH = 128;
    public static final int FAILURE_CODE_MAX_LENGTH = 64;
    public static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

    @Column(name = "operation_id", nullable = false, updatable = false)
    private UUID operationId;

    @Column(name = "provision_id", nullable = false, updatable = false)
    private UUID provisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40, updatable = false)
    private XuiClientOperationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private XuiClientOperationStatus status;

    @Column(name = "request_fingerprint", nullable = false, length = FINGERPRINT_MAX_LENGTH, updatable = false)
    private String requestFingerprint;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = FAILURE_CODE_MAX_LENGTH)
    private String failureCode;

    @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
    private String failureMessage;

    protected XuiClientOperation() {
    }

    private XuiClientOperation(
            UUID operationId,
            UUID provisionId,
            XuiClientOperationType type,
            String requestFingerprint,
            Instant requestedAt
    ) {
        this.operationId = Objects.requireNonNull(operationId, "operationId must not be null");
        this.provisionId = Objects.requireNonNull(provisionId, "provisionId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.requestFingerprint = requireText(requestFingerprint, "requestFingerprint", FINGERPRINT_MAX_LENGTH);
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.status = XuiClientOperationStatus.PENDING;
    }

    public static XuiClientOperation create(
            UUID operationId,
            UUID provisionId,
            XuiClientOperationType type,
            String requestFingerprint,
            Instant requestedAt
    ) {
        return new XuiClientOperation(operationId, provisionId, type, requestFingerprint, requestedAt);
    }

    public void markInProgress() {
        if (status == XuiClientOperationStatus.IN_PROGRESS) {
            return;
        }
        if (status != XuiClientOperationStatus.PENDING && status != XuiClientOperationStatus.UNKNOWN) {
            throw new IllegalStateException("cannot mark operation in progress from status " + status);
        }
        status = XuiClientOperationStatus.IN_PROGRESS;
        clearFailure();
    }

    public void markSucceeded(Instant completedAt) {
        Instant requiredCompletedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        if (status == XuiClientOperationStatus.SUCCEEDED) {
            return;
        }
        if (status != XuiClientOperationStatus.IN_PROGRESS && status != XuiClientOperationStatus.UNKNOWN) {
            throw new IllegalStateException("cannot mark operation succeeded from status " + status);
        }
        status = XuiClientOperationStatus.SUCCEEDED;
        this.completedAt = requiredCompletedAt;
        clearFailure();
    }

    public void markFailed(String failureCode, String safeMessage, Instant completedAt) {
        Instant requiredCompletedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        if (status == XuiClientOperationStatus.SUCCEEDED) {
            throw new IllegalStateException("cannot fail a succeeded operation");
        }
        status = XuiClientOperationStatus.FAILED;
        this.completedAt = requiredCompletedAt;
        this.failureCode = normalizeOptional(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = normalizeOptional(safeMessage, "failureMessage", FAILURE_MESSAGE_MAX_LENGTH);
    }

    public void markUnknown(String failureCode, String safeMessage) {
        if (status == XuiClientOperationStatus.SUCCEEDED) {
            return;
        }
        status = XuiClientOperationStatus.UNKNOWN;
        this.failureCode = normalizeOptional(failureCode, "failureCode", FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = normalizeOptional(safeMessage, "failureMessage", FAILURE_MESSAGE_MAX_LENGTH);
    }

    public UUID getOperationId() {
        return operationId;
    }

    public UUID getProvisionId() {
        return provisionId;
    }

    public XuiClientOperationType getType() {
        return type;
    }

    public XuiClientOperationStatus getStatus() {
        return status;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    private void clearFailure() {
        failureCode = null;
        failureMessage = null;
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
}
