package com.parazit.panel.domain.plan;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(
        name = "plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_plans_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_plans_code", columnList = "code"),
                @Index(name = "idx_plans_status", columnList = "status"),
                @Index(name = "idx_plans_type", columnList = "type"),
                @Index(name = "idx_plans_display_order", columnList = "display_order"),
                @Index(name = "idx_plans_created_at", columnList = "created_at")
        }
)
public class Plan extends BaseEntity {

    public static final int CODE_MIN_LENGTH = 3;
    public static final int CODE_MAX_LENGTH = 64;
    public static final int NAME_MAX_LENGTH = 128;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]+$");

    @Column(name = "code", nullable = false, length = CODE_MAX_LENGTH, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private PlanType type;

    @Column(name = "price_amount", nullable = false)
    private long priceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 8)
    private CurrencyCode currency;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "traffic_limit_bytes")
    private Long trafficLimitBytes;

    @Column(name = "max_devices")
    private Integer maxDevices;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "renewal_enabled", nullable = false)
    private boolean renewalEnabled;

    protected Plan() {
    }

    private Plan(
            String code,
            String name,
            String description,
            PlanType type,
            long priceAmount,
            CurrencyCode currency,
            int durationDays,
            Long trafficLimitBytes,
            Integer maxDevices,
            int displayOrder
    ) {
        this.code = normalizeCode(code);
        this.status = PlanStatus.DRAFT;
        applyDetails(name, description, type, priceAmount, currency, durationDays, trafficLimitBytes, maxDevices, displayOrder);
    }

    public static Plan create(
            String code,
            String name,
            String description,
            PlanType type,
            long priceAmount,
            CurrencyCode currency,
            int durationDays,
            Long trafficLimitBytes,
            Integer maxDevices,
            int displayOrder
    ) {
        return new Plan(code, name, description, type, priceAmount, currency, durationDays, trafficLimitBytes, maxDevices, displayOrder);
    }

    public void updateDetails(
            String name,
            String description,
            PlanType type,
            long priceAmount,
            CurrencyCode currency,
            int durationDays,
            Long trafficLimitBytes,
            Integer maxDevices,
            int displayOrder
    ) {
        if (status == PlanStatus.ARCHIVED) {
            throw new IllegalStateException("archived plans cannot be updated");
        }
        applyDetails(name, description, type, priceAmount, currency, durationDays, trafficLimitBytes, maxDevices, displayOrder);
    }

    public void activate() {
        if (status == PlanStatus.DRAFT || status == PlanStatus.INACTIVE) {
            status = PlanStatus.ACTIVE;
            return;
        }
        throw invalidTransition("activate");
    }

    public void deactivate() {
        if (status == PlanStatus.ACTIVE) {
            status = PlanStatus.INACTIVE;
            return;
        }
        throw invalidTransition("deactivate");
    }

    public void archive() {
        if (status == PlanStatus.DRAFT || status == PlanStatus.ACTIVE || status == PlanStatus.INACTIVE) {
            status = PlanStatus.ARCHIVED;
            return;
        }
        throw invalidTransition("archive");
    }

    public void enableRenewal() {
        if (status == PlanStatus.ARCHIVED) {
            throw new IllegalStateException("archived plans cannot be enabled for renewal");
        }
        renewalEnabled = true;
    }

    public void disableRenewal() {
        if (status == PlanStatus.ARCHIVED) {
            throw new IllegalStateException("archived plans cannot be disabled for renewal");
        }
        renewalEnabled = false;
    }

    public static String normalizeCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (normalized.length() < CODE_MIN_LENGTH) {
            throw new IllegalArgumentException("code must be at least " + CODE_MIN_LENGTH + " characters");
        }
        if (normalized.length() > CODE_MAX_LENGTH) {
            throw new IllegalArgumentException("code must be at most " + CODE_MAX_LENGTH + " characters");
        }
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("code may contain only uppercase letters, digits, underscores, and hyphens");
        }
        return normalized;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public PlanType getType() {
        return type;
    }

    public long getPriceAmount() {
        return priceAmount;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public Long getTrafficLimitBytes() {
        return trafficLimitBytes;
    }

    public Integer getMaxDevices() {
        return maxDevices;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isRenewalEnabled() {
        return renewalEnabled;
    }

    private void applyDetails(
            String name,
            String description,
            PlanType type,
            long priceAmount,
            CurrencyCode currency,
            int durationDays,
            Long trafficLimitBytes,
            Integer maxDevices,
            int displayOrder
    ) {
        this.name = normalizeRequired(name, "name", NAME_MAX_LENGTH);
        this.description = normalizeOptional(description, "description", DESCRIPTION_MAX_LENGTH);
        this.type = requireType(type);
        this.priceAmount = requireNonNegativePrice(priceAmount);
        this.currency = requireCurrency(currency);
        this.durationDays = requirePositiveDuration(durationDays);
        this.trafficLimitBytes = normalizeTrafficLimitBytes(type, trafficLimitBytes);
        this.maxDevices = normalizeMaxDevices(maxDevices);
        this.displayOrder = requireNonNegativeDisplayOrder(displayOrder);
    }

    private static PlanType requireType(PlanType type) {
        return Objects.requireNonNull(type, "type must not be null");
    }

    private static CurrencyCode requireCurrency(CurrencyCode currency) {
        return Objects.requireNonNull(currency, "currency must not be null");
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
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
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static long requireNonNegativePrice(long priceAmount) {
        if (priceAmount < 0) {
            throw new IllegalArgumentException("priceAmount must be zero or positive");
        }
        return priceAmount;
    }

    private static int requirePositiveDuration(int durationDays) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException("durationDays must be positive");
        }
        return durationDays;
    }

    private static Long normalizeTrafficLimitBytes(PlanType type, Long trafficLimitBytes) {
        if (type == PlanType.UNLIMITED) {
            if (trafficLimitBytes != null) {
                throw new IllegalArgumentException("trafficLimitBytes must be null for unlimited plans");
            }
            return null;
        }

        if (trafficLimitBytes == null) {
            throw new IllegalArgumentException("trafficLimitBytes is required for traffic-limited plans");
        }
        if (trafficLimitBytes <= 0) {
            throw new IllegalArgumentException("trafficLimitBytes must be positive");
        }
        return trafficLimitBytes;
    }

    private static Integer normalizeMaxDevices(Integer maxDevices) {
        if (maxDevices == null) {
            return null;
        }
        if (maxDevices <= 0) {
            throw new IllegalArgumentException("maxDevices must be positive");
        }
        return maxDevices;
    }

    private static int requireNonNegativeDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder must be zero or positive");
        }
        return displayOrder;
    }

    private IllegalStateException invalidTransition(String action) {
        return new IllegalStateException("cannot " + action + " plan with status " + status);
    }
}
