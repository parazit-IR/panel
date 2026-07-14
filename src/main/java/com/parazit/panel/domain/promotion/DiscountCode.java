package com.parazit.panel.domain.promotion;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.plan.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "discount_codes",
        indexes = {
                @Index(name = "idx_discount_codes_code_hash", columnList = "code_hash", unique = true),
                @Index(name = "idx_discount_codes_status_valid", columnList = "status,valid_from,valid_until")
        }
)
public class DiscountCode extends BaseEntity {

    public static final int CODE_HASH_MAX_LENGTH = 128;
    public static final int MASKED_CODE_MAX_LENGTH = 32;

    @Column(name = "code_hash", nullable = false, length = CODE_HASH_MAX_LENGTH, updatable = false, unique = true)
    private String codeHash;

    @Column(name = "masked_code", nullable = false, length = MASKED_CODE_MAX_LENGTH, updatable = false)
    private String maskedCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 32, updatable = false)
    private DiscountType discountType;

    @Column(name = "fixed_amount", updatable = false)
    private Long fixedAmount;

    @Column(name = "percentage_basis_points", updatable = false)
    private Integer percentageBasisPoints;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

    @Column(name = "minimum_order_amount", nullable = false, updatable = false)
    private long minimumOrderAmount;

    @Column(name = "maximum_discount_amount", updatable = false)
    private Long maximumDiscountAmount;

    @Column(name = "allow_new_subscription", nullable = false)
    private boolean allowNewSubscription;

    @Column(name = "allow_renewal", nullable = false)
    private boolean allowRenewal;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "total_usage_limit", nullable = false)
    private int totalUsageLimit;

    @Column(name = "per_user_usage_limit", nullable = false)
    private int perUserUsageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "stackable", nullable = false)
    private boolean stackable;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DiscountCodeStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected DiscountCode() {
    }

    public static DiscountCode fixed(
            String codeHash,
            String maskedCode,
            Money fixedAmount,
            Money minimumOrderAmount,
            Money maximumDiscountAmount,
            boolean allowNewSubscription,
            boolean allowRenewal,
            Instant validFrom,
            Instant validUntil,
            int totalUsageLimit,
            int perUserUsageLimit
    ) {
        DiscountCode code = new DiscountCode();
        code.codeHash = requireText(codeHash, "codeHash", CODE_HASH_MAX_LENGTH);
        code.maskedCode = requireText(maskedCode, "maskedCode", MASKED_CODE_MAX_LENGTH);
        Money amount = requirePositive(fixedAmount, "fixedAmount");
        Money minimum = requireNonNegative(minimumOrderAmount, amount.currency(), "minimumOrderAmount");
        Money maximum = maximumDiscountAmount == null ? null : requirePositive(maximumDiscountAmount, "maximumDiscountAmount");
        if (maximum != null && maximum.currency() != amount.currency()) {
            throw new IllegalArgumentException("maximumDiscountAmount currency mismatch");
        }
        code.discountType = DiscountType.FIXED_AMOUNT;
        code.fixedAmount = amount.amount();
        code.percentageBasisPoints = null;
        code.currency = amount.currency().name();
        code.minimumOrderAmount = minimum.amount();
        code.maximumDiscountAmount = maximum == null ? null : maximum.amount();
        code.allowNewSubscription = allowNewSubscription;
        code.allowRenewal = allowRenewal;
        code.initialize(validFrom, validUntil, totalUsageLimit, perUserUsageLimit);
        return code;
    }

    public static DiscountCode percentage(
            String codeHash,
            String maskedCode,
            int percentageBasisPoints,
            CurrencyCode currency,
            Money minimumOrderAmount,
            Money maximumDiscountAmount,
            boolean allowNewSubscription,
            boolean allowRenewal,
            Instant validFrom,
            Instant validUntil,
            int totalUsageLimit,
            int perUserUsageLimit
    ) {
        if (percentageBasisPoints <= 0 || percentageBasisPoints > 10_000) {
            throw new IllegalArgumentException("percentageBasisPoints must be between 1 and 10000");
        }
        CurrencyCode requiredCurrency = Objects.requireNonNull(currency, "currency must not be null");
        Money minimum = requireNonNegative(minimumOrderAmount, requiredCurrency, "minimumOrderAmount");
        Money maximum = maximumDiscountAmount == null ? null : requirePositive(maximumDiscountAmount, "maximumDiscountAmount");
        if (maximum != null && maximum.currency() != requiredCurrency) {
            throw new IllegalArgumentException("maximumDiscountAmount currency mismatch");
        }
        DiscountCode code = new DiscountCode();
        code.codeHash = requireText(codeHash, "codeHash", CODE_HASH_MAX_LENGTH);
        code.maskedCode = requireText(maskedCode, "maskedCode", MASKED_CODE_MAX_LENGTH);
        code.discountType = DiscountType.PERCENTAGE;
        code.fixedAmount = null;
        code.percentageBasisPoints = percentageBasisPoints;
        code.currency = requiredCurrency.name();
        code.minimumOrderAmount = minimum.amount();
        code.maximumDiscountAmount = maximum == null ? null : maximum.amount();
        code.allowNewSubscription = allowNewSubscription;
        code.allowRenewal = allowRenewal;
        code.initialize(validFrom, validUntil, totalUsageLimit, perUserUsageLimit);
        return code;
    }

    private void initialize(Instant validFrom, Instant validUntil, int totalUsageLimit, int perUserUsageLimit) {
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom must not be null");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil must not be null");
        if (!this.validUntil.isAfter(this.validFrom)) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        if (totalUsageLimit < 0 || perUserUsageLimit <= 0) {
            throw new IllegalArgumentException("usage limits are invalid");
        }
        if (totalUsageLimit > 0 && perUserUsageLimit > totalUsageLimit) {
            throw new IllegalArgumentException("perUserUsageLimit cannot exceed totalUsageLimit");
        }
        this.totalUsageLimit = totalUsageLimit;
        this.perUserUsageLimit = perUserUsageLimit;
        this.usedCount = 0;
        this.stackable = false;
        this.active = true;
        this.status = DiscountCodeStatus.ACTIVE;
    }

    public void reserveUse() {
        if (!active || status != DiscountCodeStatus.ACTIVE) {
            throw new IllegalStateException("discount code is not active");
        }
        if (totalUsageLimit > 0 && usedCount >= totalUsageLimit) {
            status = DiscountCodeStatus.EXHAUSTED;
            throw new IllegalStateException("discount code exhausted");
        }
        usedCount++;
        if (totalUsageLimit > 0 && usedCount >= totalUsageLimit) {
            status = DiscountCodeStatus.EXHAUSTED;
        }
    }

    public void releaseUse() {
        if (usedCount > 0) {
            usedCount--;
        }
        if (active && status == DiscountCodeStatus.EXHAUSTED
                && (totalUsageLimit == 0 || usedCount < totalUsageLimit)) {
            status = DiscountCodeStatus.ACTIVE;
        }
    }

    public boolean eligibleFor(OrderType orderType) {
        return switch (Objects.requireNonNull(orderType, "orderType must not be null")) {
            case NEW_SUBSCRIPTION -> allowNewSubscription;
            case RENEWAL -> allowRenewal;
            case TRAFFIC_ADDON -> false;
        };
    }

    public Money fixedMoney() {
        return fixedAmount == null ? null : new Money(fixedAmount, currencyCode());
    }

    public Money minimumOrderMoney() {
        return new Money(minimumOrderAmount, currencyCode());
    }

    public Money maximumDiscountMoney() {
        return maximumDiscountAmount == null ? null : new Money(maximumDiscountAmount, currencyCode());
    }

    public CurrencyCode currencyCode() {
        return CurrencyCode.valueOf(currency);
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getMaskedCode() {
        return maskedCode;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public Long getFixedAmount() {
        return fixedAmount;
    }

    public Integer getPercentageBasisPoints() {
        return percentageBasisPoints;
    }

    public String getCurrency() {
        return currency;
    }

    public long getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public Long getMaximumDiscountAmount() {
        return maximumDiscountAmount;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public int getTotalUsageLimit() {
        return totalUsageLimit;
    }

    public int getPerUserUsageLimit() {
        return perUserUsageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public boolean isStackable() {
        return stackable;
    }

    public DiscountCodeStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "DiscountCode[id=" + getId() + ", maskedCode=" + maskedCode + ", status=" + status + ']';
    }

    private static Money requirePositive(Money money, String field) {
        Money required = Objects.requireNonNull(money, field + " must not be null");
        if (required.amount() <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return required;
    }

    private static Money requireNonNegative(Money money, CurrencyCode currency, String field) {
        if (money == null) {
            return new Money(0, currency);
        }
        if (money.currency() != currency) {
            throw new IllegalArgumentException(field + " currency mismatch");
        }
        return money;
    }

    private static String requireText(String value, String field, int maxLength) {
        String normalized = Objects.requireNonNull(value, field + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
