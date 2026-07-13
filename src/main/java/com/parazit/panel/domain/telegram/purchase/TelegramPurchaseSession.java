package com.parazit.panel.domain.telegram.purchase;

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
        name = "telegram_purchase_sessions",
        indexes = {
                @Index(name = "idx_telegram_purchase_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_telegram_purchase_sessions_telegram_user", columnList = "telegram_user_id"),
                @Index(name = "idx_telegram_purchase_sessions_selection", columnList = "plan_selection_id"),
                @Index(name = "idx_telegram_purchase_sessions_order", columnList = "order_id"),
                @Index(name = "idx_telegram_purchase_sessions_payment", columnList = "payment_id"),
                @Index(name = "idx_telegram_purchase_sessions_status", columnList = "status"),
                @Index(name = "idx_telegram_purchase_sessions_expires_at", columnList = "expires_at")
        }
)
public class TelegramPurchaseSession extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "telegram_user_id", nullable = false, updatable = false)
    private Long telegramUserId;

    @Column(name = "plan_selection_id", nullable = false)
    private UUID planSelectionId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private TelegramPurchaseSessionStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TelegramPurchaseSession() {
    }

    private TelegramPurchaseSession(UUID userId, long telegramUserId, UUID planSelectionId, Instant expiresAt) {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.telegramUserId = telegramUserId;
        this.planSelectionId = Objects.requireNonNull(planSelectionId, "planSelectionId must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = TelegramPurchaseSessionStatus.PLAN_SELECTED;
    }

    public static TelegramPurchaseSession create(UUID userId, long telegramUserId, UUID planSelectionId, Instant expiresAt) {
        return new TelegramPurchaseSession(userId, telegramUserId, planSelectionId, expiresAt);
    }

    public boolean activeAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !isTerminal() && now.isBefore(expiresAt);
    }

    public boolean expiredAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !isTerminal() && !now.isBefore(expiresAt);
    }

    public void showPreInvoice() {
        if (status == TelegramPurchaseSessionStatus.PLAN_SELECTED) {
            status = TelegramPurchaseSessionStatus.PRE_INVOICE_SHOWN;
        }
    }

    public void attachOrder(UUID orderId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        if (status == TelegramPurchaseSessionStatus.PLAN_SELECTED || status == TelegramPurchaseSessionStatus.PRE_INVOICE_SHOWN) {
            status = TelegramPurchaseSessionStatus.ORDER_CREATED;
        }
    }

    public void showPaymentMethods() {
        if (status == TelegramPurchaseSessionStatus.ORDER_CREATED) {
            status = TelegramPurchaseSessionStatus.PAYMENT_METHODS_SHOWN;
        }
    }

    public void attachPayment(UUID paymentId) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId must not be null");
        if (!isTerminal()) {
            status = TelegramPurchaseSessionStatus.PAYMENT_CREATED;
        }
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (!isTerminal()) {
            status = TelegramPurchaseSessionStatus.EXPIRED;
        }
    }

    public void cancel() {
        if (!isTerminal()) {
            status = TelegramPurchaseSessionStatus.CANCELLED;
        }
    }

    public boolean isTerminal() {
        return status == TelegramPurchaseSessionStatus.COMPLETED
                || status == TelegramPurchaseSessionStatus.EXPIRED
                || status == TelegramPurchaseSessionStatus.CANCELLED;
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public UUID getPlanSelectionId() {
        return planSelectionId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public TelegramPurchaseSessionStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "TelegramPurchaseSession[id=%s,status=%s]".formatted(getId(), status);
    }
}
