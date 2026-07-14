package com.parazit.panel.domain.wallet.topup;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_top_up_requests",
        indexes = {
                @Index(name = "idx_wallet_top_up_requests_user_status_created", columnList = "user_id,status,created_at"),
                @Index(name = "idx_wallet_top_up_requests_wallet_status", columnList = "wallet_id,status"),
                @Index(name = "idx_wallet_top_up_requests_payment_id", columnList = "payment_id")
        }
)
public class WalletTopUpRequest extends BaseEntity {

    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 160;
    public static final int FAILED_REASON_MAX_LENGTH = 120;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Column(name = "requested_amount", nullable = false, updatable = false)
    private long requestedAmount;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private WalletTopUpStatus status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "idempotency_key", nullable = false, length = IDEMPOTENCY_KEY_MAX_LENGTH, updatable = false)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "credited_at")
    private Instant creditedAt;

    @Column(name = "failed_reason", length = FAILED_REASON_MAX_LENGTH)
    private String failedReason;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected WalletTopUpRequest() {
    }

    private WalletTopUpRequest(
            UUID userId,
            UUID walletId,
            Money requestedAmount,
            String idempotencyKey,
            Instant now,
            Duration ttl
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.walletId = Objects.requireNonNull(walletId, "walletId must not be null");
        Money amount = requirePositive(requestedAmount);
        this.requestedAmount = amount.amount();
        this.currency = amount.currency().name();
        this.status = WalletTopUpStatus.AWAITING_PAYMENT_METHOD;
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey", IDEMPOTENCY_KEY_MAX_LENGTH);
        Instant createdAt = Objects.requireNonNull(now, "now must not be null");
        Duration requestTtl = Objects.requireNonNull(ttl, "ttl must not be null");
        if (requestTtl.isZero() || requestTtl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.expiresAt = createdAt.plus(requestTtl);
    }

    public static WalletTopUpRequest create(
            UUID userId,
            UUID walletId,
            Money requestedAmount,
            String idempotencyKey,
            Instant now,
            Duration ttl
    ) {
        return new WalletTopUpRequest(userId, walletId, requestedAmount, idempotencyKey, now, ttl);
    }

    public void attachPayment(UUID paymentId, Instant now) {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(now, "now must not be null");
        if (this.paymentId != null) {
            if (!this.paymentId.equals(paymentId)) {
                throw new IllegalStateException("wallet top-up already has a different payment");
            }
            return;
        }
        requireStatus(WalletTopUpStatus.AWAITING_PAYMENT_METHOD, "attach payment");
        if (!expiresAt.isAfter(now)) {
            expire(now);
            throw new IllegalStateException("wallet top-up request is expired");
        }
        this.paymentId = paymentId;
        this.status = WalletTopUpStatus.PENDING_PAYMENT;
    }

    public void markPaymentApproved(Instant approvedAt) {
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        if (status == WalletTopUpStatus.PAYMENT_APPROVED || status == WalletTopUpStatus.CREDITED) {
            return;
        }
        requireStatus(WalletTopUpStatus.PENDING_PAYMENT, "mark payment approved");
        status = WalletTopUpStatus.PAYMENT_APPROVED;
    }

    public void markCredited(Instant creditedAt) {
        Objects.requireNonNull(creditedAt, "creditedAt must not be null");
        if (status == WalletTopUpStatus.CREDITED) {
            return;
        }
        if (status != WalletTopUpStatus.PENDING_PAYMENT && status != WalletTopUpStatus.PAYMENT_APPROVED) {
            throw invalidTransition("mark credited");
        }
        status = WalletTopUpStatus.CREDITED;
        this.creditedAt = creditedAt;
        failedReason = null;
    }

    public void fail(String reason) {
        if (status == WalletTopUpStatus.CREDITED) {
            throw invalidTransition("fail");
        }
        status = WalletTopUpStatus.FAILED;
        failedReason = normalizeOptional(reason, FAILED_REASON_MAX_LENGTH);
    }

    public void cancel(Instant cancelledAt) {
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        if (status == WalletTopUpStatus.CANCELLED) {
            return;
        }
        if (paymentId != null || status == WalletTopUpStatus.CREDITED) {
            throw invalidTransition("cancel");
        }
        status = WalletTopUpStatus.CANCELLED;
    }

    public void expire(Instant expiredAt) {
        Objects.requireNonNull(expiredAt, "expiredAt must not be null");
        if (status == WalletTopUpStatus.EXPIRED) {
            return;
        }
        if (status != WalletTopUpStatus.AWAITING_PAYMENT_METHOD) {
            throw invalidTransition("expire");
        }
        status = WalletTopUpStatus.EXPIRED;
    }

    public boolean isTerminal() {
        return status == WalletTopUpStatus.CREDITED
                || status == WalletTopUpStatus.CANCELLED
                || status == WalletTopUpStatus.EXPIRED
                || status == WalletTopUpStatus.FAILED;
    }

    public boolean matchesSemanticRequest(Money amount) {
        Money required = Objects.requireNonNull(amount, "amount must not be null");
        return requestedAmount == required.amount() && currencyCode() == required.currency();
    }

    public Money requestedMoney() {
        return new Money(requestedAmount, currencyCode());
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public long getRequestedAmount() {
        return requestedAmount;
    }

    public CurrencyCode currencyCode() {
        return CurrencyCode.valueOf(currency);
    }

    public String getCurrency() {
        return currency;
    }

    public WalletTopUpStatus getStatus() {
        return status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreditedAt() {
        return creditedAt;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "WalletTopUpRequest[id=" + getId()
                + ", userId=" + userId
                + ", walletId=" + walletId
                + ", status=" + status
                + "]";
    }

    private void requireStatus(WalletTopUpStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("cannot " + action + " wallet top-up request with status " + status);
    }

    private static Money requirePositive(Money amount) {
        Money required = Objects.requireNonNull(amount, "amount must not be null");
        if (required.amount() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return required;
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
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
