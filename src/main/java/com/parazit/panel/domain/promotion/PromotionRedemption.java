package com.parazit.panel.domain.promotion;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
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
        name = "promotion_redemptions",
        indexes = {
                @Index(name = "idx_promotion_redemptions_user_discount", columnList = "user_id,discount_code_id"),
                @Index(name = "idx_promotion_redemptions_user_gift", columnList = "user_id,gift_code_id"),
                @Index(name = "idx_promotion_redemptions_order", columnList = "order_id"),
                @Index(name = "idx_promotion_redemptions_wallet_transaction", columnList = "wallet_transaction_id", unique = true),
                @Index(name = "idx_promotion_redemptions_status_created", columnList = "status,created_at")
        }
)
public class PromotionRedemption extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", nullable = false, length = 32, updatable = false)
    private PromotionCodeType codeType;

    @Column(name = "discount_code_id", updatable = false)
    private UUID discountCodeId;

    @Column(name = "gift_code_id", updatable = false)
    private UUID giftCodeId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Column(name = "wallet_id")
    private UUID walletId;

    @Column(name = "wallet_transaction_id")
    private UUID walletTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PromotionRedemptionStatus status;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "original_amount")
    private Long originalAmount;

    @Column(name = "discount_amount")
    private Long discountAmount;

    @Column(name = "final_amount")
    private Long finalAmount;

    @Column(name = "gift_amount")
    private Long giftAmount;

    @Column(name = "idempotency_key", nullable = false, length = 160, updatable = false)
    private String idempotencyKey;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    protected PromotionRedemption() {
    }

    public static PromotionRedemption reserveDiscount(
            UUID discountCodeId,
            UUID userId,
            UUID orderId,
            Money originalAmount,
            Money discountAmount,
            Money finalAmount,
            String idempotencyKey,
            Instant now
    ) {
        Money original = Objects.requireNonNull(originalAmount, "originalAmount must not be null");
        Money discount = Objects.requireNonNull(discountAmount, "discountAmount must not be null");
        Money fin = Objects.requireNonNull(finalAmount, "finalAmount must not be null");
        requireSameCurrency(original, discount, fin);
        PromotionRedemption redemption = new PromotionRedemption();
        redemption.codeType = PromotionCodeType.DISCOUNT;
        redemption.discountCodeId = Objects.requireNonNull(discountCodeId, "discountCodeId must not be null");
        redemption.giftCodeId = null;
        redemption.userId = Objects.requireNonNull(userId, "userId must not be null");
        redemption.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        redemption.status = PromotionRedemptionStatus.RESERVED;
        redemption.currency = original.currency().name();
        redemption.originalAmount = original.amount();
        redemption.discountAmount = discount.amount();
        redemption.finalAmount = fin.amount();
        redemption.giftAmount = null;
        redemption.idempotencyKey = requireText(idempotencyKey, "idempotencyKey", 160);
        redemption.redeemedAt = Objects.requireNonNull(now, "now must not be null");
        return redemption;
    }

    public static PromotionRedemption applyGift(
            UUID giftCodeId,
            UUID userId,
            UUID walletId,
            Money giftAmount,
            String idempotencyKey,
            Instant now
    ) {
        Money gift = Objects.requireNonNull(giftAmount, "giftAmount must not be null");
        PromotionRedemption redemption = new PromotionRedemption();
        redemption.codeType = PromotionCodeType.GIFT;
        redemption.discountCodeId = null;
        redemption.giftCodeId = Objects.requireNonNull(giftCodeId, "giftCodeId must not be null");
        redemption.userId = Objects.requireNonNull(userId, "userId must not be null");
        redemption.walletId = Objects.requireNonNull(walletId, "walletId must not be null");
        redemption.status = PromotionRedemptionStatus.APPLIED;
        redemption.currency = gift.currency().name();
        redemption.giftAmount = gift.amount();
        redemption.idempotencyKey = requireText(idempotencyKey, "idempotencyKey", 160);
        redemption.redeemedAt = Objects.requireNonNull(now, "now must not be null");
        return redemption;
    }

    public void consume(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == PromotionRedemptionStatus.CONSUMED) {
            return;
        }
        requireStatus(PromotionRedemptionStatus.RESERVED, "consume");
        status = PromotionRedemptionStatus.CONSUMED;
    }

    public void release(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == PromotionRedemptionStatus.RELEASED) {
            return;
        }
        requireStatus(PromotionRedemptionStatus.RESERVED, "release");
        status = PromotionRedemptionStatus.RELEASED;
        releasedAt = now;
    }

    public void attachWalletTransaction(UUID walletTransactionId) {
        UUID required = Objects.requireNonNull(walletTransactionId, "walletTransactionId must not be null");
        if (this.walletTransactionId != null && !this.walletTransactionId.equals(required)) {
            throw new IllegalStateException("wallet transaction already attached");
        }
        this.walletTransactionId = required;
    }

    public Money originalMoney() {
        return originalAmount == null ? null : new Money(originalAmount, currencyCode());
    }

    public Money discountMoney() {
        return discountAmount == null ? null : new Money(discountAmount, currencyCode());
    }

    public Money finalMoney() {
        return finalAmount == null ? null : new Money(finalAmount, currencyCode());
    }

    public Money giftMoney() {
        return giftAmount == null ? null : new Money(giftAmount, currencyCode());
    }

    public CurrencyCode currencyCode() {
        return CurrencyCode.valueOf(currency);
    }

    public PromotionCodeType getCodeType() {
        return codeType;
    }

    public UUID getDiscountCodeId() {
        return discountCodeId;
    }

    public UUID getGiftCodeId() {
        return giftCodeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getWalletTransactionId() {
        return walletTransactionId;
    }

    public PromotionRedemptionStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getOriginalAmount() {
        return originalAmount;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public Long getFinalAmount() {
        return finalAmount;
    }

    public Long getGiftAmount() {
        return giftAmount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    @Override
    public String toString() {
        return "PromotionRedemption[id=" + getId() + ", codeType=" + codeType + ", status=" + status + ']';
    }

    private void requireStatus(PromotionRedemptionStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException("cannot " + action + " redemption with status " + status);
        }
    }

    private static void requireSameCurrency(Money first, Money second, Money third) {
        if (first.currency() != second.currency() || first.currency() != third.currency()) {
            throw new IllegalArgumentException("money currency mismatch");
        }
    }

    private static String requireText(String value, String field, int maxLength) {
        String normalized = Objects.requireNonNull(value, field + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return normalized;
    }
}
