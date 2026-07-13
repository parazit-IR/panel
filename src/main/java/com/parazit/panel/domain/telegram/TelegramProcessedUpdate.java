package com.parazit.panel.domain.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "telegram_processed_updates",
        indexes = {
                @Index(name = "idx_telegram_processed_updates_status", columnList = "status"),
                @Index(name = "idx_telegram_processed_updates_received_at", columnList = "received_at"),
                @Index(name = "idx_telegram_processed_updates_processed_at", columnList = "processed_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class TelegramProcessedUpdate {

    public static final int HANDLER_KEY_MAX_LENGTH = 100;
    public static final int FAILURE_CODE_MAX_LENGTH = 64;
    public static final int FAILURE_MESSAGE_MAX_LENGTH = 500;
    public static final int RESPONSE_FINGERPRINT_MAX_LENGTH = 128;

    @Id
    @Column(name = "update_id", nullable = false, updatable = false)
    private Long updateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TelegramUpdateProcessingStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "handler_key", length = HANDLER_KEY_MAX_LENGTH)
    private String handlerKey;

    @Column(name = "failure_code", length = FAILURE_CODE_MAX_LENGTH)
    private String failureCode;

    @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
    private String failureMessage;

    @Column(name = "response_fingerprint", length = RESPONSE_FINGERPRINT_MAX_LENGTH)
    private String responseFingerprint;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TelegramProcessedUpdate() {
    }

    private TelegramProcessedUpdate(long updateId, Instant receivedAt) {
        if (updateId < 0) {
            throw new IllegalArgumentException("updateId must be non-negative");
        }
        this.updateId = updateId;
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        this.status = TelegramUpdateProcessingStatus.RECEIVED;
        this.attemptCount = 0;
    }

    public static TelegramProcessedUpdate receive(long updateId, Instant receivedAt) {
        return new TelegramProcessedUpdate(updateId, receivedAt);
    }

    public boolean canClaim(int maxAttempts) {
        if (status == TelegramUpdateProcessingStatus.PROCESSED || status == TelegramUpdateProcessingStatus.DEAD) {
            return false;
        }
        if (status == TelegramUpdateProcessingStatus.PROCESSING) {
            return false;
        }
        return attemptCount < maxAttempts;
    }

    public void claim(Instant now, String handlerKey, int maxAttempts) {
        Objects.requireNonNull(now, "now must not be null");
        if (!canClaim(maxAttempts)) {
            throw new IllegalStateException("update cannot be claimed");
        }
        this.status = TelegramUpdateProcessingStatus.PROCESSING;
        this.processingStartedAt = now;
        this.handlerKey = bounded(handlerKey, HANDLER_KEY_MAX_LENGTH);
        this.attemptCount = Math.addExact(attemptCount, 1);
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markProcessed(Instant now, String responseFingerprint) {
        Objects.requireNonNull(now, "now must not be null");
        this.status = TelegramUpdateProcessingStatus.PROCESSED;
        this.processedAt = now;
        this.responseFingerprint = bounded(responseFingerprint, RESPONSE_FINGERPRINT_MAX_LENGTH);
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(Instant now, String failureCode, String failureMessage, int maxAttempts) {
        Objects.requireNonNull(now, "now must not be null");
        this.failedAt = now;
        this.failureCode = bounded(failureCode, FAILURE_CODE_MAX_LENGTH);
        this.failureMessage = bounded(failureMessage, FAILURE_MESSAGE_MAX_LENGTH);
        this.status = attemptCount >= maxAttempts ? TelegramUpdateProcessingStatus.DEAD : TelegramUpdateProcessingStatus.FAILED;
    }

    public Long getUpdateId() {
        return updateId;
    }

    public TelegramUpdateProcessingStatus getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getHandlerKey() {
        return handlerKey;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getResponseFingerprint() {
        return responseFingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "TelegramProcessedUpdate[updateId=%d,status=%s,attemptCount=%d]"
                .formatted(updateId, status, attemptCount);
    }

    private static String bounded(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
