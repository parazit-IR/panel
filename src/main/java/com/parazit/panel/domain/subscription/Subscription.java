package com.parazit.panel.domain.subscription;

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
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subscriptions_xui_client_provision", columnNames = "xui_client_provision_id"),
                @UniqueConstraint(name = "uq_subscriptions_access_token_hash", columnNames = "access_token_hash")
        },
        indexes = {
                @Index(name = "idx_subscriptions_user_id", columnList = "user_id"),
                @Index(name = "idx_subscriptions_order_id", columnList = "order_id"),
                @Index(name = "idx_subscriptions_status", columnList = "status"),
                @Index(name = "idx_subscriptions_expires_at", columnList = "expires_at"),
                @Index(name = "idx_subscriptions_access_token_prefix", columnList = "access_token_prefix"),
                @Index(name = "idx_subscriptions_remote_client_id", columnList = "remote_client_id")
        }
)
public class Subscription extends BaseEntity {

    public static final int ACCESS_TOKEN_HASH_LENGTH = 64;
    public static final int ACCESS_TOKEN_PREFIX_MAX_LENGTH = 20;
    public static final int REVOKE_REASON_MAX_LENGTH = 500;
    public static final int DISPLAY_NAME_MAX_LENGTH = 200;
    public static final int CONTENT_VERSION_MAX_LENGTH = 32;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "plan_selection_id", nullable = false, updatable = false)
    private UUID planSelectionId;

    @Column(name = "xui_client_provision_id", nullable = false, updatable = false)
    private UUID xuiClientProvisionId;

    @Column(name = "inbound_id", nullable = false, updatable = false)
    private long inboundId;

    @Column(name = "remote_client_id", nullable = false, updatable = false)
    private UUID remoteClientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "access_token_hash", nullable = false, length = ACCESS_TOKEN_HASH_LENGTH)
    private String accessTokenHash;

    @Column(name = "access_token_prefix", nullable = false, length = ACCESS_TOKEN_PREFIX_MAX_LENGTH)
    private String accessTokenPrefix;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    private long accessCount;

    @Column(name = "revoke_reason", length = REVOKE_REASON_MAX_LENGTH)
    private String revokeReason;

    @Column(name = "display_name", length = DISPLAY_NAME_MAX_LENGTH)
    private String displayName;

    @Column(name = "content_version", length = CONTENT_VERSION_MAX_LENGTH)
    private String contentVersion;

    protected Subscription() {
    }

    private Subscription(
            UUID userId,
            UUID orderId,
            UUID planSelectionId,
            UUID xuiClientProvisionId,
            long inboundId,
            UUID remoteClientId,
            String accessTokenHash,
            String accessTokenPrefix,
            Instant activatedAt,
            Instant expiresAt,
            String displayName,
            String contentVersion
    ) {
        this.userId = requireUuid(userId, "userId");
        this.orderId = requireUuid(orderId, "orderId");
        this.planSelectionId = requireUuid(planSelectionId, "planSelectionId");
        this.xuiClientProvisionId = requireUuid(xuiClientProvisionId, "xuiClientProvisionId");
        this.inboundId = requirePositive(inboundId, "inboundId");
        this.remoteClientId = requireUuid(remoteClientId, "remoteClientId");
        this.accessTokenHash = requireTokenHash(accessTokenHash);
        this.accessTokenPrefix = requireText(accessTokenPrefix, "accessTokenPrefix", ACCESS_TOKEN_PREFIX_MAX_LENGTH);
        this.tokenVersion = 1;
        this.status = SubscriptionStatus.ACTIVE;
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt must not be null");
        this.expiresAt = expiresAt;
        this.accessCount = 0;
        this.displayName = normalizeOptional(displayName, "displayName", DISPLAY_NAME_MAX_LENGTH);
        this.contentVersion = normalizeOptional(contentVersion, "contentVersion", CONTENT_VERSION_MAX_LENGTH);
    }

    public static Subscription activate(
            UUID userId,
            UUID orderId,
            UUID planSelectionId,
            UUID xuiClientProvisionId,
            long inboundId,
            UUID remoteClientId,
            String accessTokenHash,
            String accessTokenPrefix,
            Instant activatedAt,
            Instant expiresAt,
            String displayName,
            String contentVersion
    ) {
        return new Subscription(
                userId,
                orderId,
                planSelectionId,
                xuiClientProvisionId,
                inboundId,
                remoteClientId,
                accessTokenHash,
                accessTokenPrefix,
                activatedAt,
                expiresAt,
                displayName,
                contentVersion
        );
    }

    public void rotateToken(String accessTokenHash, String accessTokenPrefix) {
        if (isTerminal()) {
            throw new IllegalStateException("cannot rotate token for terminal subscription");
        }
        this.accessTokenHash = requireTokenHash(accessTokenHash);
        this.accessTokenPrefix = requireText(accessTokenPrefix, "accessTokenPrefix", ACCESS_TOKEN_PREFIX_MAX_LENGTH);
        tokenVersion = Math.addExact(tokenVersion, 1);
    }

    public void suspend() {
        if (status == SubscriptionStatus.SUSPENDED) {
            return;
        }
        if (status != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("cannot suspend subscription with status " + status);
        }
        status = SubscriptionStatus.SUSPENDED;
    }

    public void resume(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == SubscriptionStatus.ACTIVE) {
            return;
        }
        if (status != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("cannot resume subscription with status " + status);
        }
        if (isExpiredAt(now)) {
            expire(now);
            throw new IllegalStateException("cannot resume expired subscription");
        }
        status = SubscriptionStatus.ACTIVE;
    }

    public void revoke(Instant revokedAt, String reason) {
        Instant requiredRevokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        if (status == SubscriptionStatus.REVOKED) {
            if (this.revokeReason == null) {
                this.revokeReason = normalizeOptional(reason, "revokeReason", REVOKE_REASON_MAX_LENGTH);
            }
            return;
        }
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("cannot revoke subscription with status " + status);
        }
        status = SubscriptionStatus.REVOKED;
        this.revokedAt = requiredRevokedAt;
        this.revokeReason = normalizeOptional(reason, "revokeReason", REVOKE_REASON_MAX_LENGTH);
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == SubscriptionStatus.EXPIRED) {
            return;
        }
        if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("cannot expire subscription with status " + status);
        }
        status = SubscriptionStatus.EXPIRED;
    }

    public void applyRenewal(Instant newExpiresAt, Instant now) {
        Instant requiredExpiresAt = Objects.requireNonNull(newExpiresAt, "newExpiresAt must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (status == SubscriptionStatus.REVOKED || status == SubscriptionStatus.INVALID) {
            throw new IllegalStateException("cannot renew subscription with status " + status);
        }
        if (status == SubscriptionStatus.SUSPENDED) {
            throw new IllegalStateException("cannot automatically renew suspended subscription");
        }
        if (!requiredExpiresAt.isAfter(now)) {
            throw new IllegalArgumentException("newExpiresAt must be in the future");
        }
        expiresAt = requiredExpiresAt;
        status = SubscriptionStatus.ACTIVE;
        if (activatedAt == null) {
            activatedAt = now;
        }
    }

    public void markInvalid() {
        if (status == SubscriptionStatus.INVALID) {
            return;
        }
        if (status == SubscriptionStatus.REVOKED || status == SubscriptionStatus.EXPIRED) {
            throw new IllegalStateException("cannot invalidate terminal subscription with status " + status);
        }
        status = SubscriptionStatus.INVALID;
    }

    public void recordAccess(Instant accessedAt) {
        lastAccessedAt = Objects.requireNonNull(accessedAt, "accessedAt must not be null");
        accessCount = Math.addExact(accessCount, 1);
    }

    public boolean isAccessibleAt(Instant now) {
        return status == SubscriptionStatus.ACTIVE && !isExpiredAt(now);
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isTerminal() {
        return status == SubscriptionStatus.REVOKED
                || status == SubscriptionStatus.EXPIRED
                || status == SubscriptionStatus.INVALID;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getPlanSelectionId() {
        return planSelectionId;
    }

    public UUID getXuiClientProvisionId() {
        return xuiClientProvisionId;
    }

    public long getInboundId() {
        return inboundId;
    }

    public UUID getRemoteClientId() {
        return remoteClientId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public String getAccessTokenPrefix() {
        return accessTokenPrefix;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getContentVersion() {
        return contentVersion;
    }

    @Override
    public String toString() {
        return "Subscription[id=" + getId()
                + ", userId=" + userId
                + ", provisionId=" + xuiClientProvisionId
                + ", status=" + status
                + ", tokenVersion=" + tokenVersion
                + "]";
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

    private static String requireTokenHash(String value) {
        String normalized = requireText(value, "accessTokenHash", ACCESS_TOKEN_HASH_LENGTH);
        if (normalized.length() != ACCESS_TOKEN_HASH_LENGTH || !normalized.matches("^[a-f0-9]{64}$")) {
            throw new IllegalArgumentException("accessTokenHash must be lowercase SHA-256 hex");
        }
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
}
