package com.parazit.panel.domain.telegram;

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

@Entity
@Table(
        name = "telegram_sensitive_actions",
        indexes = {
                @Index(name = "idx_telegram_sensitive_actions_user", columnList = "telegram_user_id"),
                @Index(name = "idx_telegram_sensitive_actions_subscription", columnList = "subscription_id"),
                @Index(name = "idx_telegram_sensitive_actions_status", columnList = "status"),
                @Index(name = "idx_telegram_sensitive_actions_expires_at", columnList = "expires_at")
        }
)
public class TelegramSensitiveAction extends BaseEntity {

    public static final int RESULT_FINGERPRINT_MAX_LENGTH = 128;

    @Column(name = "telegram_user_id", nullable = false, updatable = false)
    private Long telegramUserId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private TelegramSensitiveActionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TelegramSensitiveActionStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "result_fingerprint", length = RESULT_FINGERPRINT_MAX_LENGTH)
    private String resultFingerprint;

    protected TelegramSensitiveAction() {
    }

    private TelegramSensitiveAction(
            long telegramUserId,
            UUID subscriptionId,
            TelegramSensitiveActionType type,
            Instant expiresAt
    ) {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        this.telegramUserId = telegramUserId;
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.status = TelegramSensitiveActionStatus.PENDING;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public static TelegramSensitiveAction pendingRotation(long telegramUserId, UUID subscriptionId, Instant expiresAt) {
        return new TelegramSensitiveAction(
                telegramUserId,
                subscriptionId,
                TelegramSensitiveActionType.ROTATE_SUBSCRIPTION_TOKEN,
                expiresAt
        );
    }

    public boolean isPendingAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return status == TelegramSensitiveActionStatus.PENDING && now.isBefore(expiresAt);
    }

    public void complete(Instant now, String resultFingerprint) {
        Objects.requireNonNull(now, "now must not be null");
        if (!isPendingAt(now)) {
            throw new IllegalStateException("sensitive action is not pending");
        }
        this.status = TelegramSensitiveActionStatus.COMPLETED;
        this.completedAt = now;
        this.resultFingerprint = bounded(resultFingerprint);
    }

    public void cancel() {
        if (status == TelegramSensitiveActionStatus.PENDING) {
            this.status = TelegramSensitiveActionStatus.CANCELLED;
        }
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == TelegramSensitiveActionStatus.PENDING && !now.isBefore(expiresAt)) {
            this.status = TelegramSensitiveActionStatus.EXPIRED;
        }
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public TelegramSensitiveActionType getType() {
        return type;
    }

    public TelegramSensitiveActionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getResultFingerprint() {
        return resultFingerprint;
    }

    @Override
    public String toString() {
        return "TelegramSensitiveAction[id=%s,type=%s,status=%s]".formatted(getId(), type, status);
    }

    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= RESULT_FINGERPRINT_MAX_LENGTH
                ? normalized
                : normalized.substring(0, RESULT_FINGERPRINT_MAX_LENGTH);
    }
}
