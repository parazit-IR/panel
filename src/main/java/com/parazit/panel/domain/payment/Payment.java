package com.parazit.panel.domain.payment;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order_id", columnList = "order_id"),
                @Index(name = "idx_payments_user_id", columnList = "user_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_method", columnList = "method")
        }
)
public class Payment extends BaseEntity {

    public static final int CURRENCY_MAX_LENGTH = 8;
    public static final int GATEWAY_ID_MAX_LENGTH = 128;
    public static final int REJECTION_REASON_MAX_LENGTH = 500;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40, updatable = false)
    private PaymentTargetType targetType;

    @Column(name = "wallet_top_up_request_id", updatable = false)
    private UUID walletTopUpRequestId;

    @Column(name = "wallet_transaction_id")
    private UUID walletTransactionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 32, updatable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "base_amount", nullable = false, updatable = false)
    private long baseAmount;

    @Column(name = "payable_amount", nullable = false, updatable = false)
    private long payableAmount;

    @Column(name = "currency", nullable = false, length = CURRENCY_MAX_LENGTH, updatable = false)
    private String currency;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "gateway_transaction_id", length = GATEWAY_ID_MAX_LENGTH)
    private String gatewayTransactionId;

    @Column(name = "gateway_authority", length = GATEWAY_ID_MAX_LENGTH)
    private String gatewayAuthority;

    @Column(name = "rejection_reason", length = REJECTION_REASON_MAX_LENGTH)
    private String rejectionReason;

    protected Payment() {
    }

    private Payment(
            UUID orderId,
            UUID walletTopUpRequestId,
            PaymentTargetType targetType,
            UUID userId,
            PaymentMethod method,
            long baseAmount,
            long payableAmount,
            String currency,
            Instant expiresAt
    ) {
        PaymentTargetType requiredTargetType = Objects.requireNonNull(targetType, "targetType must not be null");
        if (requiredTargetType == PaymentTargetType.ORDER) {
            this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
            this.walletTopUpRequestId = null;
            this.baseAmount = requireNonNegative(baseAmount, "baseAmount");
        } else {
            this.orderId = null;
            this.walletTopUpRequestId = Objects.requireNonNull(walletTopUpRequestId, "walletTopUpRequestId must not be null");
            this.baseAmount = requirePositive(baseAmount, "baseAmount");
        }
        this.targetType = requiredTargetType;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.payableAmount = requirePayableAmount(payableAmount, baseAmount);
        this.currency = normalizeCurrency(currency);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = PaymentStatus.CREATED;
    }

    public static Payment create(
            UUID orderId,
            UUID userId,
            PaymentMethod method,
            long baseAmount,
            long payableAmount,
            String currency,
            Instant expiresAt
    ) {
        return new Payment(orderId, null, PaymentTargetType.ORDER, userId, method, baseAmount, payableAmount, currency, expiresAt);
    }

    public static Payment createWalletTopUp(
            UUID userId,
            UUID walletTopUpRequestId,
            PaymentMethod method,
            long baseAmount,
            long payableAmount,
            String currency,
            Instant expiresAt
    ) {
        return new Payment(null, walletTopUpRequestId, PaymentTargetType.WALLET_TOP_UP, userId, method, baseAmount, payableAmount, currency, expiresAt);
    }

    public void markWaitingForPayment() {
        if (status == PaymentStatus.WAITING_FOR_PAYMENT) {
            return;
        }
        requireStatus(PaymentStatus.CREATED, "wait for payment");
        status = PaymentStatus.WAITING_FOR_PAYMENT;
    }

    public void returnToWaitingForPaymentAfterReviewRejection() {
        if (status == PaymentStatus.WAITING_FOR_PAYMENT) {
            return;
        }
        requireStatus(PaymentStatus.WAITING_FOR_REVIEW, "return to waiting for payment");
        status = PaymentStatus.WAITING_FOR_PAYMENT;
    }

    public void markProcessing() {
        if (status == PaymentStatus.PROCESSING) {
            return;
        }
        if (status != PaymentStatus.CREATED
                && status != PaymentStatus.WAITING_FOR_PAYMENT
                && status != PaymentStatus.WAITING_FOR_REVIEW) {
            throw invalidTransition("process");
        }
        status = PaymentStatus.PROCESSING;
    }

    public void markReceiptSubmitted(Instant submittedAt) {
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        if (status == PaymentStatus.RECEIPT_SUBMITTED) {
            return;
        }
        requireStatus(PaymentStatus.WAITING_FOR_PAYMENT, "mark receipt submitted");
        status = PaymentStatus.RECEIPT_SUBMITTED;
    }

    public void markWaitingForReview() {
        if (status == PaymentStatus.WAITING_FOR_REVIEW) {
            return;
        }
        requireStatus(PaymentStatus.RECEIPT_SUBMITTED, "wait for review");
        status = PaymentStatus.WAITING_FOR_REVIEW;
    }

    public void markApproved(Instant approvedAt, String gatewayTransactionId, String gatewayAuthority) {
        if (status == PaymentStatus.APPROVED) {
            return;
        }
        if (status != PaymentStatus.WAITING_FOR_PAYMENT
                && status != PaymentStatus.PROCESSING
                && status != PaymentStatus.WAITING_FOR_REVIEW
                && status != PaymentStatus.UNKNOWN) {
            throw invalidTransition("approve");
        }
        Instant timestamp = Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        status = PaymentStatus.APPROVED;
        paidAt = timestamp;
        this.approvedAt = timestamp;
        this.gatewayTransactionId = normalizeOptional(gatewayTransactionId, "gatewayTransactionId", GATEWAY_ID_MAX_LENGTH);
        this.gatewayAuthority = normalizeOptional(gatewayAuthority, "gatewayAuthority", GATEWAY_ID_MAX_LENGTH);
        rejectedAt = null;
        rejectionReason = null;
    }

    public void attachWalletTransaction(UUID walletTransactionId) {
        if (method != PaymentMethod.WALLET) {
            throw new IllegalStateException("wallet transaction can only be attached to wallet payments");
        }
        UUID requiredTransactionId = Objects.requireNonNull(walletTransactionId, "walletTransactionId must not be null");
        if (this.walletTransactionId != null && !this.walletTransactionId.equals(requiredTransactionId)) {
            throw new IllegalStateException("wallet transaction is already attached");
        }
        this.walletTransactionId = requiredTransactionId;
    }

    public void markRejected(Instant rejectedAt, String reason) {
        if (status == PaymentStatus.REJECTED) {
            return;
        }
        if (status != PaymentStatus.WAITING_FOR_PAYMENT && status != PaymentStatus.PROCESSING) {
            throw invalidTransition("reject");
        }
        status = PaymentStatus.REJECTED;
        this.rejectedAt = Objects.requireNonNull(rejectedAt, "rejectedAt must not be null");
        this.rejectionReason = normalizeOptional(reason, "rejectionReason", REJECTION_REASON_MAX_LENGTH);
    }

    public void markExpired(Instant expiredAt) {
        Objects.requireNonNull(expiredAt, "expiredAt must not be null");
        if (status == PaymentStatus.EXPIRED) {
            return;
        }
        if (status != PaymentStatus.CREATED && status != PaymentStatus.WAITING_FOR_PAYMENT) {
            throw invalidTransition("expire");
        }
        status = PaymentStatus.EXPIRED;
    }

    public void markCancelled(Instant cancelledAt) {
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        if (status == PaymentStatus.CANCELLED) {
            return;
        }
        if (status != PaymentStatus.CREATED && status != PaymentStatus.WAITING_FOR_PAYMENT) {
            throw invalidTransition("cancel");
        }
        status = PaymentStatus.CANCELLED;
    }

    public void markFailed(Instant failedAt, String reason) {
        Objects.requireNonNull(failedAt, "failedAt must not be null");
        if (status == PaymentStatus.FAILED) {
            return;
        }
        if (isTerminal()) {
            throw invalidTransition("fail");
        }
        status = PaymentStatus.FAILED;
        rejectionReason = normalizeOptional(reason, "failureReason", REJECTION_REASON_MAX_LENGTH);
    }

    public void markUnknown(Instant unknownAt, String reason) {
        Objects.requireNonNull(unknownAt, "unknownAt must not be null");
        if (status == PaymentStatus.UNKNOWN) {
            return;
        }
        if (status == PaymentStatus.APPROVED) {
            return;
        }
        if (status != PaymentStatus.WAITING_FOR_PAYMENT && status != PaymentStatus.PROCESSING) {
            throw invalidTransition("mark unknown");
        }
        status = PaymentStatus.UNKNOWN;
        rejectionReason = normalizeOptional(reason, "unknownReason", REJECTION_REASON_MAX_LENGTH);
    }

    public void resumeProcessingFromUnknown() {
        if (status == PaymentStatus.PROCESSING) {
            return;
        }
        requireStatus(PaymentStatus.UNKNOWN, "resume processing");
        status = PaymentStatus.PROCESSING;
    }

    public boolean isTerminal() {
        return status == PaymentStatus.APPROVED
                || status == PaymentStatus.REJECTED
                || status == PaymentStatus.EXPIRED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public PaymentTargetType getTargetType() {
        return targetType == null ? PaymentTargetType.ORDER : targetType;
    }

    public UUID getWalletTopUpRequestId() {
        return walletTopUpRequestId;
    }

    public UUID getWalletTransactionId() {
        return walletTransactionId;
    }

    public boolean targetsOrder() {
        return getTargetType() == PaymentTargetType.ORDER;
    }

    public boolean targetsWalletTopUp() {
        return getTargetType() == PaymentTargetType.WALLET_TOP_UP;
    }

    public UUID getUserId() {
        return userId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public long getPayableAmount() {
        return payableAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public String getGatewayAuthority() {
        return gatewayAuthority;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    private void requireStatus(PaymentStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("cannot " + action + " payment with status " + status);
    }

    private static long requirePayableAmount(long payableAmount, long baseAmount) {
        requireNonNegative(payableAmount, "payableAmount");
        if (payableAmount < baseAmount) {
            throw new IllegalArgumentException("payableAmount must be greater than or equal to baseAmount");
        }
        return payableAmount;
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

    private static String normalizeCurrency(String currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        if (normalized.length() > CURRENCY_MAX_LENGTH) {
            throw new IllegalArgumentException("currency must be at most " + CURRENCY_MAX_LENGTH + " characters");
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
