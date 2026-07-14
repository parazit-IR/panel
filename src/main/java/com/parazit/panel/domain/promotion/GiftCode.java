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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "gift_codes",
        indexes = {
                @Index(name = "idx_gift_codes_code_hash", columnList = "code_hash", unique = true),
                @Index(name = "idx_gift_codes_status_valid", columnList = "status,valid_from,valid_until")
        }
)
public class GiftCode extends BaseEntity {

    @Column(name = "code_hash", nullable = false, length = 128, updatable = false, unique = true)
    private String codeHash;

    @Column(name = "masked_code", nullable = false, length = 32, updatable = false)
    private String maskedCode;

    @Column(name = "credit_amount", nullable = false, updatable = false)
    private long creditAmount;

    @Column(name = "currency", nullable = false, length = 8, updatable = false)
    private String currency;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GiftCodeStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected GiftCode() {
    }

    public static GiftCode create(
            String codeHash,
            String maskedCode,
            Money creditAmount,
            Instant validFrom,
            Instant validUntil,
            int totalUsageLimit,
            int perUserUsageLimit
    ) {
        Money amount = Objects.requireNonNull(creditAmount, "creditAmount must not be null");
        if (amount.amount() <= 0) {
            throw new IllegalArgumentException("creditAmount must be positive");
        }
        GiftCode code = new GiftCode();
        code.codeHash = requireText(codeHash, "codeHash", 128);
        code.maskedCode = requireText(maskedCode, "maskedCode", 32);
        code.creditAmount = amount.amount();
        code.currency = amount.currency().name();
        code.validFrom = Objects.requireNonNull(validFrom, "validFrom must not be null");
        code.validUntil = Objects.requireNonNull(validUntil, "validUntil must not be null");
        if (!code.validUntil.isAfter(code.validFrom)) {
            throw new IllegalArgumentException("validUntil must be after validFrom");
        }
        if (totalUsageLimit < 0 || perUserUsageLimit <= 0) {
            throw new IllegalArgumentException("usage limits are invalid");
        }
        if (totalUsageLimit > 0 && perUserUsageLimit > totalUsageLimit) {
            throw new IllegalArgumentException("perUserUsageLimit cannot exceed totalUsageLimit");
        }
        code.totalUsageLimit = totalUsageLimit;
        code.perUserUsageLimit = perUserUsageLimit;
        code.usedCount = 0;
        code.active = true;
        code.status = GiftCodeStatus.ACTIVE;
        return code;
    }

    public void reserveUse() {
        if (!active || status != GiftCodeStatus.ACTIVE) {
            throw new IllegalStateException("gift code is not active");
        }
        if (totalUsageLimit > 0 && usedCount >= totalUsageLimit) {
            status = GiftCodeStatus.EXHAUSTED;
            throw new IllegalStateException("gift code exhausted");
        }
        usedCount++;
        if (totalUsageLimit > 0 && usedCount >= totalUsageLimit) {
            status = GiftCodeStatus.EXHAUSTED;
        }
    }

    public Money creditMoney() {
        return new Money(creditAmount, currencyCode());
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

    public long getCreditAmount() {
        return creditAmount;
    }

    public String getCurrency() {
        return currency;
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

    public GiftCodeStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "GiftCode[id=" + getId() + ", maskedCode=" + maskedCode + ", status=" + status + ']';
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
