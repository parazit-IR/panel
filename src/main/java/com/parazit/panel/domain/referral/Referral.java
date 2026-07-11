package com.parazit.panel.domain.referral;

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
        name = "referrals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_referrals_referred_user_id", columnNames = "referred_user_id")
        },
        indexes = {
                @Index(name = "idx_referrals_referrer_user_id", columnList = "referrer_user_id"),
                @Index(name = "idx_referrals_referred_user_id", columnList = "referred_user_id"),
                @Index(name = "idx_referrals_status", columnList = "status"),
                @Index(name = "idx_referrals_referred_at", columnList = "referred_at")
        }
)
public class Referral extends BaseEntity {

    @Column(name = "referrer_user_id", nullable = false, updatable = false)
    private UUID referrerUserId;

    @Column(name = "referred_user_id", nullable = false, updatable = false)
    private UUID referredUserId;

    @Column(name = "referral_code_used", nullable = false, length = ReferralCodePolicy.MAX_LENGTH, updatable = false)
    private String referralCodeUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReferralStatus status;

    @Column(name = "referred_at", nullable = false, updatable = false)
    private Instant referredAt;

    protected Referral() {
    }

    private Referral(UUID referrerUserId, UUID referredUserId, String referralCodeUsed, Instant referredAt) {
        this.referrerUserId = requireUserId(referrerUserId, "referrerUserId");
        this.referredUserId = requireUserId(referredUserId, "referredUserId");
        if (this.referrerUserId.equals(this.referredUserId)) {
            throw new IllegalArgumentException("a user cannot refer themselves");
        }
        this.referralCodeUsed = ReferralCodePolicy.normalizeAndValidate(referralCodeUsed);
        this.status = ReferralStatus.PENDING;
        this.referredAt = Objects.requireNonNull(referredAt, "referredAt must not be null");
    }

    public static Referral create(UUID referrerUserId, UUID referredUserId, String referralCodeUsed, Instant referredAt) {
        return new Referral(referrerUserId, referredUserId, referralCodeUsed, referredAt);
    }

    public void confirm() {
        if (status != ReferralStatus.PENDING) {
            throw new IllegalStateException("only pending referrals can be confirmed");
        }
        status = ReferralStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != ReferralStatus.PENDING) {
            throw new IllegalStateException("only pending referrals can be cancelled");
        }
        status = ReferralStatus.CANCELLED;
    }

    public UUID getReferrerUserId() {
        return referrerUserId;
    }

    public UUID getReferredUserId() {
        return referredUserId;
    }

    public String getReferralCodeUsed() {
        return referralCodeUsed;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public Instant getReferredAt() {
        return referredAt;
    }

    private UUID requireUserId(UUID userId, String fieldName) {
        return Objects.requireNonNull(userId, fieldName + " must not be null");
    }
}
