package com.parazit.panel.domain.wallet;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
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
        name = "wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_wallet_transactions_idempotency", columnNames = {"wallet_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_wallet_transactions_wallet_occurred", columnList = "wallet_id,occurred_at,id"),
                @Index(name = "idx_wallet_transactions_user_occurred", columnList = "user_id,occurred_at,id"),
                @Index(name = "idx_wallet_transactions_reference", columnList = "reference_type,reference_id")
        }
)
public class WalletTransaction extends BaseEntity {

    public static final int REFERENCE_TYPE_MAX_LENGTH = 80;
    public static final int IDEMPOTENCY_KEY_MAX_LENGTH = 160;
    public static final int DESCRIPTION_CODE_MAX_LENGTH = 120;

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 48, updatable = false)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16, updatable = false)
    private WalletTransactionDirection direction;

    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

    @Column(name = "balance_before", nullable = false, updatable = false)
    private long balanceBefore;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private long balanceAfter;

    @Column(name = "reference_type", nullable = false, length = REFERENCE_TYPE_MAX_LENGTH, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "idempotency_key", nullable = false, length = IDEMPOTENCY_KEY_MAX_LENGTH, updatable = false)
    private String idempotencyKey;

    @Column(name = "description_code", length = DESCRIPTION_CODE_MAX_LENGTH, updatable = false)
    private String descriptionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32, updatable = false)
    private WalletTransactionStatus status;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected WalletTransaction() {
    }

    private WalletTransaction(
            UUID walletId,
            UUID userId,
            WalletTransactionType type,
            WalletTransactionDirection direction,
            Money amount,
            Money balanceBefore,
            Money balanceAfter,
            String referenceType,
            UUID referenceId,
            String idempotencyKey,
            String descriptionCode,
            Instant occurredAt
    ) {
        this.walletId = Objects.requireNonNull(walletId, "walletId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        Money requiredAmount = requirePositive(amount, "amount");
        Money requiredBefore = Objects.requireNonNull(balanceBefore, "balanceBefore must not be null");
        Money requiredAfter = Objects.requireNonNull(balanceAfter, "balanceAfter must not be null");
        requireSameCurrency(requiredAmount, requiredBefore, requiredAfter);
        verifyEquation(direction, requiredAmount, requiredBefore, requiredAfter);
        this.amount = requiredAmount.amount();
        this.currency = requiredAmount.currency().name();
        this.balanceBefore = requiredBefore.amount();
        this.balanceAfter = requiredAfter.amount();
        this.referenceType = requireText(referenceType, "referenceType", REFERENCE_TYPE_MAX_LENGTH);
        this.referenceId = referenceId;
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey", IDEMPOTENCY_KEY_MAX_LENGTH);
        this.descriptionCode = normalizeOptional(descriptionCode, DESCRIPTION_CODE_MAX_LENGTH);
        this.status = WalletTransactionStatus.POSTED;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static WalletTransaction post(
            UUID walletId,
            UUID userId,
            WalletTransactionType type,
            WalletTransactionDirection direction,
            Money amount,
            Money balanceBefore,
            Money balanceAfter,
            String referenceType,
            UUID referenceId,
            String idempotencyKey,
            String descriptionCode,
            Instant occurredAt
    ) {
        return new WalletTransaction(walletId, userId, type, direction, amount, balanceBefore, balanceAfter,
                referenceType, referenceId, idempotencyKey, descriptionCode, occurredAt);
    }

    public boolean semanticallyMatches(
            WalletTransactionType type,
            WalletTransactionDirection direction,
            Money amount,
            String referenceType,
            UUID referenceId,
            String descriptionCode
    ) {
        return this.type == type
                && this.direction == direction
                && this.amount == amount.amount()
                && currencyCode() == amount.currency()
                && this.referenceType.equals(requireText(referenceType, "referenceType", REFERENCE_TYPE_MAX_LENGTH))
                && Objects.equals(this.referenceId, referenceId)
                && Objects.equals(this.descriptionCode, normalizeOptional(descriptionCode, DESCRIPTION_CODE_MAX_LENGTH));
    }

    public Money amount() {
        return new Money(amount, currencyCode());
    }

    public Money balanceBefore() {
        return new Money(balanceBefore, currencyCode());
    }

    public Money balanceAfter() {
        return new Money(balanceAfter, currencyCode());
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getUserId() {
        return userId;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public WalletTransactionDirection getDirection() {
        return direction;
    }

    public long getAmount() {
        return amount;
    }

    public CurrencyCode currencyCode() {
        return CurrencyCode.valueOf(currency);
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceBeforeAmount() {
        return balanceBefore;
    }

    public long getBalanceAfterAmount() {
        return balanceAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getDescriptionCode() {
        return descriptionCode;
    }

    public WalletTransactionStatus getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "WalletTransaction[id=" + getId()
                + ", walletId=" + walletId
                + ", userId=" + userId
                + ", type=" + type
                + ", direction=" + direction
                + ", status=" + status
                + "]";
    }

    private static Money requirePositive(Money value, String fieldName) {
        Money money = Objects.requireNonNull(value, fieldName + " must not be null");
        if (money.amount() <= 0) {
            throw new InvalidWalletAmountException();
        }
        return money;
    }

    private static void requireSameCurrency(Money amount, Money before, Money after) {
        CurrencyCode currency = amount.currency();
        if (before.currency() != currency || after.currency() != currency) {
            throw new WalletCurrencyMismatchException();
        }
    }

    private static void verifyEquation(WalletTransactionDirection direction, Money amount, Money before, Money after) {
        long expected = switch (direction) {
            case CREDIT -> Math.addExact(before.amount(), amount.amount());
            case DEBIT -> before.amount() - amount.amount();
        };
        if (expected < 0 || expected != after.amount()) {
            throw new IllegalArgumentException("wallet transaction balance equation is invalid");
        }
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
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
