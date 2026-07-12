package com.parazit.panel.domain.payment.manual.review;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "manual_payment_reviews",
        indexes = {
                @Index(name = "idx_manual_reviews_payment_id", columnList = "payment_id"),
                @Index(name = "idx_manual_reviews_order_id", columnList = "order_id"),
                @Index(name = "idx_manual_reviews_status", columnList = "status"),
                @Index(name = "idx_manual_reviews_reviewer_id", columnList = "reviewer_id"),
                @Index(name = "idx_manual_reviews_claimed_at", columnList = "claimed_at"),
                @Index(name = "idx_manual_reviews_decided_at", columnList = "decided_at")
        }
)
public class ManualPaymentReview extends BaseEntity {

    public static final int REVIEWER_ID_MAX_LENGTH = 128;
    public static final int DECISION_REASON_MAX_LENGTH = 64;
    public static final int OPERATOR_NOTE_MAX_LENGTH = 1000;

    @Column(name = "receipt_id", nullable = false, updatable = false)
    private UUID receiptId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ManualPaymentReviewStatus status;

    @Column(name = "reviewer_id", length = REVIEWER_ID_MAX_LENGTH)
    private String reviewerId;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "review_started_at")
    private Instant reviewStartedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_reason", length = DECISION_REASON_MAX_LENGTH)
    private ManualPaymentRejectionReason decisionReason;

    @Column(name = "operator_note", length = OPERATOR_NOTE_MAX_LENGTH)
    private String operatorNote;

    @Column(name = "expected_amount", nullable = false, updatable = false)
    private long expectedAmount;

    @Column(name = "claimed_amount", nullable = false, updatable = false)
    private long claimedAmount;

    @Column(name = "amount_matched", nullable = false, updatable = false)
    private boolean amountMatched;

    @Column(name = "duplicate_hash_detected", nullable = false, updatable = false)
    private boolean duplicateHashDetected;

    protected ManualPaymentReview() {
    }

    private ManualPaymentReview(
            UUID receiptId,
            UUID paymentId,
            UUID orderId,
            long expectedAmount,
            long claimedAmount,
            boolean duplicateHashDetected
    ) {
        this.receiptId = Objects.requireNonNull(receiptId, "receiptId must not be null");
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.expectedAmount = requirePositive(expectedAmount, "expectedAmount");
        this.claimedAmount = requirePositive(claimedAmount, "claimedAmount");
        this.amountMatched = expectedAmount == claimedAmount;
        this.duplicateHashDetected = duplicateHashDetected;
        this.status = ManualPaymentReviewStatus.PENDING;
    }

    public static ManualPaymentReview create(
            UUID receiptId,
            UUID paymentId,
            UUID orderId,
            long expectedAmount,
            long claimedAmount,
            boolean duplicateHashDetected
    ) {
        return new ManualPaymentReview(receiptId, paymentId, orderId, expectedAmount, claimedAmount, duplicateHashDetected);
    }

    public boolean isClaimExpired(Instant now, Duration claimTtl) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(claimTtl, "claimTtl must not be null");
        return status == ManualPaymentReviewStatus.CLAIMED
                && claimedAt != null
                && !claimedAt.plus(claimTtl).isAfter(now);
    }

    public void claim(String reviewerId, Instant claimedAt) {
        String operator = normalizeRequired(reviewerId, "reviewerId", REVIEWER_ID_MAX_LENGTH);
        Instant timestamp = Objects.requireNonNull(claimedAt, "claimedAt must not be null");
        if (status == ManualPaymentReviewStatus.CLAIMED && operator.equals(this.reviewerId)) {
            return;
        }
        if (status != ManualPaymentReviewStatus.PENDING && status != ManualPaymentReviewStatus.RELEASED) {
            throw invalidTransition("claim");
        }
        this.reviewerId = operator;
        this.claimedAt = timestamp;
        this.reviewStartedAt = timestamp;
        this.status = ManualPaymentReviewStatus.CLAIMED;
    }

    public void release(String reviewerId) {
        requireReviewer(reviewerId);
        if (status == ManualPaymentReviewStatus.RELEASED) {
            return;
        }
        requireStatus(ManualPaymentReviewStatus.CLAIMED, "release");
        status = ManualPaymentReviewStatus.RELEASED;
    }

    public void approve(String reviewerId, String operatorNote, Instant decidedAt) {
        requireReviewer(reviewerId);
        if (status == ManualPaymentReviewStatus.APPROVED) {
            return;
        }
        requireStatus(ManualPaymentReviewStatus.CLAIMED, "approve");
        this.operatorNote = normalizeOptional(operatorNote, OPERATOR_NOTE_MAX_LENGTH);
        this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        this.status = ManualPaymentReviewStatus.APPROVED;
    }

    public void reject(
            String reviewerId,
            ManualPaymentRejectionReason reason,
            String operatorNote,
            Instant decidedAt
    ) {
        requireReviewer(reviewerId);
        if (status == ManualPaymentReviewStatus.REJECTED) {
            return;
        }
        requireStatus(ManualPaymentReviewStatus.CLAIMED, "reject");
        this.decisionReason = Objects.requireNonNull(reason, "reason must not be null");
        this.operatorNote = normalizeOptional(operatorNote, OPERATOR_NOTE_MAX_LENGTH);
        this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        this.status = ManualPaymentReviewStatus.REJECTED;
    }

    public UUID getReceiptId() {
        return receiptId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public ManualPaymentReviewStatus getStatus() {
        return status;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public Instant getReviewStartedAt() {
        return reviewStartedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public ManualPaymentRejectionReason getDecisionReason() {
        return decisionReason;
    }

    public String getOperatorNote() {
        return operatorNote;
    }

    public long getExpectedAmount() {
        return expectedAmount;
    }

    public long getClaimedAmount() {
        return claimedAmount;
    }

    public boolean isAmountMatched() {
        return amountMatched;
    }

    public boolean isDuplicateHashDetected() {
        return duplicateHashDetected;
    }

    private void requireReviewer(String reviewerId) {
        String operator = normalizeRequired(reviewerId, "reviewerId", REVIEWER_ID_MAX_LENGTH);
        if (!operator.equals(this.reviewerId)) {
            throw new IllegalStateException("manual payment review is claimed by another operator");
        }
    }

    private void requireStatus(ManualPaymentReviewStatus expected, String operation) {
        if (status != expected) {
            throw invalidTransition(operation);
        }
    }

    private IllegalStateException invalidTransition(String operation) {
        return new IllegalStateException("cannot " + operation + " manual payment review with status " + status);
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
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
