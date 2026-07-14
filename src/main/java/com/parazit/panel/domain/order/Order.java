package com.parazit.panel.domain.order;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_status", columnList = "status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        }
)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "plan_selection_id")
    private UUID planSelectionId;

    @Column(name = "target_subscription_id")
    private UUID targetSubscriptionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "renewal_snapshot", columnDefinition = "jsonb")
    private RenewalSnapshot renewalSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "base_amount", nullable = false)
    private long baseAmount;

    @Column(name = "final_amount", nullable = false)
    private long finalAmount;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "applied_discount_code_id")
    private UUID appliedDiscountCodeId;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    protected Order() {
    }

    private Order(UUID userId, long amount, String currency) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.amount = requirePositiveOrZero(amount, "amount");
        this.baseAmount = this.amount;
        this.finalAmount = this.amount;
        this.discountAmount = 0L;
        this.currency = normalizeCurrency(currency);
        this.type = OrderType.NEW_SUBSCRIPTION;
        this.status = OrderStatus.CREATED;
    }

    private Order(UUID userId, UUID planId, UUID planSelectionId, OrderType type, long amount, String currency) {
        this(userId, amount, currency);
        this.planId = Objects.requireNonNull(planId, "planId must not be null");
        this.planSelectionId = Objects.requireNonNull(planSelectionId, "planSelectionId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    private Order(
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            UUID targetSubscriptionId,
            RenewalSnapshot renewalSnapshot,
            long amount,
            String currency
    ) {
        this(userId, planId, planSelectionId, OrderType.RENEWAL, amount, currency);
        this.targetSubscriptionId = Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        this.renewalSnapshot = Objects.requireNonNull(renewalSnapshot, "renewalSnapshot must not be null");
        if (!this.targetSubscriptionId.equals(renewalSnapshot.targetSubscriptionId())) {
            throw new IllegalArgumentException("targetSubscriptionId must match renewal snapshot");
        }
        if (!this.planId.equals(renewalSnapshot.sourcePlanId())) {
            throw new IllegalArgumentException("planId must match renewal snapshot source plan");
        }
        if (this.finalAmount != renewalSnapshot.finalAmount().amount()) {
            throw new IllegalArgumentException("order amount must match renewal snapshot final amount");
        }
        if (!this.currency.equals(renewalSnapshot.finalAmount().currency().name())) {
            throw new IllegalArgumentException("order currency must match renewal snapshot final amount currency");
        }
    }

    public static Order create(UUID userId, long amount, String currency) {
        return new Order(userId, amount, currency);
    }

    public static Order createForPlanSelection(
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            long amount,
            String currency
    ) {
        return new Order(userId, planId, planSelectionId, OrderType.NEW_SUBSCRIPTION, amount, currency);
    }

    public static Order createRenewal(
            UUID userId,
            UUID planId,
            UUID planSelectionId,
            UUID targetSubscriptionId,
            RenewalSnapshot renewalSnapshot,
            long amount,
            String currency
    ) {
        return new Order(userId, planId, planSelectionId, targetSubscriptionId, renewalSnapshot, amount, currency);
    }

    public void markPaymentPending() {
        if (status == OrderStatus.PAYMENT_PENDING) {
            return;
        }
        requireStatus(OrderStatus.CREATED, "mark payment pending");
        status = OrderStatus.PAYMENT_PENDING;
    }

    public void applyDiscount(UUID discountCodeId, long discountAmount) {
        Objects.requireNonNull(discountCodeId, "discountCodeId must not be null");
        requireDiscountable();
        long requiredDiscount = requirePositiveOrZero(discountAmount, "discountAmount");
        if (requiredDiscount <= 0 || requiredDiscount >= baseAmount) {
            throw new IllegalArgumentException("discountAmount must be positive and less than baseAmount");
        }
        this.discountAmount = requiredDiscount;
        this.finalAmount = baseAmount - requiredDiscount;
        this.appliedDiscountCodeId = discountCodeId;
    }

    public void removeDiscount() {
        requireDiscountable();
        this.discountAmount = 0L;
        this.finalAmount = baseAmount;
        this.appliedDiscountCodeId = null;
    }

    public void markPaid(Instant paidAt) {
        if (status == OrderStatus.PAID) {
            return;
        }
        if (status == OrderStatus.RENEWAL_PENDING || status == OrderStatus.RENEWAL_REVIEW_REQUIRED) {
            return;
        }
        if (status != OrderStatus.CREATED && status != OrderStatus.PAYMENT_PENDING) {
            throw invalidTransition("mark paid");
        }
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt must not be null");
        status = OrderStatus.PAID;
        failureCode = null;
        failureMessage = null;
    }

    public void markRenewalPending(Instant paidAt) {
        if (status == OrderStatus.RENEWAL_PENDING) {
            return;
        }
        if (type != OrderType.RENEWAL) {
            throw invalidTransition("mark renewal pending");
        }
        if (status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_PENDING) {
            markPaid(paidAt);
        }
        requireStatus(OrderStatus.PAID, "mark renewal pending");
        status = OrderStatus.RENEWAL_PENDING;
        failureCode = null;
        failureMessage = null;
    }

    public void markRenewalReviewRequired(String failureCode, String failureMessage, Instant paidAt) {
        Objects.requireNonNull(paidAt, "paidAt must not be null");
        if (type != OrderType.RENEWAL) {
            throw invalidTransition("mark renewal review required");
        }
        if (status == OrderStatus.RENEWAL_REVIEW_REQUIRED) {
            this.failureCode = normalizeOptional(failureCode, 64);
            this.failureMessage = normalizeOptional(failureMessage, 500);
            return;
        }
        if (status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_PENDING) {
            markPaid(paidAt);
        }
        requireStatus(OrderStatus.PAID, "mark renewal review required");
        status = OrderStatus.RENEWAL_REVIEW_REQUIRED;
        this.failureCode = normalizeOptional(failureCode, 64);
        this.failureMessage = normalizeOptional(failureMessage, 500);
    }

    public void markProvisioning(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (!requiresProvisioning()) {
            throw invalidTransition("mark provisioning");
        }
        if (status == OrderStatus.PROVISIONING) {
            return;
        }
        if (status != OrderStatus.PAID && status != OrderStatus.PROVISIONING_FAILED) {
            throw invalidTransition("mark provisioning");
        }
        status = OrderStatus.PROVISIONING;
    }

    public void markCompleted(Instant completedAt) {
        if (status == OrderStatus.COMPLETED) {
            return;
        }
        requireStatus(OrderStatus.PROVISIONING, "complete");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        status = OrderStatus.COMPLETED;
        failureCode = null;
        failureMessage = null;
    }

    public void markRenewalCompleted(Instant completedAt) {
        if (status == OrderStatus.COMPLETED) {
            return;
        }
        if (type != OrderType.RENEWAL) {
            throw invalidTransition("complete renewal");
        }
        requireStatus(OrderStatus.RENEWAL_PENDING, "complete renewal");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        status = OrderStatus.COMPLETED;
        failureCode = null;
        failureMessage = null;
    }

    public void markRenewalExecutionReviewRequired(String failureCode, String failureMessage, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (type != OrderType.RENEWAL) {
            throw invalidTransition("mark renewal review required");
        }
        if (status == OrderStatus.RENEWAL_REVIEW_REQUIRED) {
            this.failureCode = normalizeOptional(failureCode, 64);
            this.failureMessage = normalizeOptional(failureMessage, 500);
            return;
        }
        if (status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_PENDING || status == OrderStatus.PAID) {
            markRenewalReviewRequired(failureCode, failureMessage, now);
            return;
        }
        requireStatus(OrderStatus.RENEWAL_PENDING, "mark renewal review required");
        status = OrderStatus.RENEWAL_REVIEW_REQUIRED;
        this.failureCode = normalizeOptional(failureCode, 64);
        this.failureMessage = normalizeOptional(failureMessage, 500);
    }

    public void markProvisioningFailed(String failureCode, String failureMessage) {
        if (status == OrderStatus.PROVISIONING_FAILED) {
            this.failureCode = normalizeOptional(failureCode, 64);
            this.failureMessage = normalizeOptional(failureMessage, 500);
            return;
        }
        requireStatus(OrderStatus.PROVISIONING, "mark provisioning failed");
        status = OrderStatus.PROVISIONING_FAILED;
        this.failureCode = normalizeOptional(failureCode, 64);
        this.failureMessage = normalizeOptional(failureMessage, 500);
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            return;
        }
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("cannot cancel order with status " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    public void markExpired() {
        if (status == OrderStatus.EXPIRED) {
            return;
        }
        if (status != OrderStatus.CREATED && status != OrderStatus.PAYMENT_PENDING) {
            throw invalidTransition("expire");
        }
        status = OrderStatus.EXPIRED;
    }

    public boolean requiresProvisioning() {
        return type == OrderType.NEW_SUBSCRIPTION && planId != null && planSelectionId != null;
    }

    public boolean isRenewal() {
        return type == OrderType.RENEWAL;
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

    public UUID getTargetSubscriptionId() {
        return targetSubscriptionId;
    }

    public RenewalSnapshot getRenewalSnapshot() {
        return renewalSnapshot;
    }

    public OrderType getType() {
        return type;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public long getFinalAmount() {
        return finalAmount;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public UUID getAppliedDiscountCodeId() {
        return appliedDiscountCodeId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
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

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected) {
            throw invalidTransition(action);
        }
    }

    private void requireDiscountable() {
        if (status != OrderStatus.CREATED && status != OrderStatus.PAYMENT_PENDING) {
            throw invalidTransition("change discount");
        }
        if (paidAt != null) {
            throw invalidTransition("change discount");
        }
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("cannot " + action + " order with status " + status);
    }

    private static long requirePositiveOrZero(long value, String fieldName) {
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
        if (normalized.length() > 8) {
            throw new IllegalArgumentException("currency must be at most 8 characters");
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
