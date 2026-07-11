package com.parazit.panel.domain.plan.selection;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
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
        name = "plan_selections",
        indexes = {
                @Index(name = "idx_plan_selections_user_id", columnList = "user_id"),
                @Index(name = "idx_plan_selections_plan_id", columnList = "plan_id"),
                @Index(name = "idx_plan_selections_status", columnList = "status"),
                @Index(name = "idx_plan_selections_expires_at", columnList = "expires_at"),
                @Index(name = "idx_plan_selections_selected_at", columnList = "selected_at")
        }
)
public class PlanSelection extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "plan_code_snapshot", nullable = false, length = Plan.CODE_MAX_LENGTH, updatable = false)
    private String planCodeSnapshot;

    @Column(name = "plan_name_snapshot", nullable = false, length = Plan.NAME_MAX_LENGTH, updatable = false)
    private String planNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type_snapshot", nullable = false, length = 32, updatable = false)
    private PlanType planTypeSnapshot;

    @Column(name = "price_amount_snapshot", nullable = false, updatable = false)
    private long priceAmountSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_snapshot", nullable = false, length = 8, updatable = false)
    private CurrencyCode currencySnapshot;

    @Column(name = "duration_days_snapshot", nullable = false, updatable = false)
    private int durationDaysSnapshot;

    @Column(name = "traffic_limit_bytes_snapshot", updatable = false)
    private Long trafficLimitBytesSnapshot;

    @Column(name = "max_devices_snapshot", updatable = false)
    private Integer maxDevicesSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PlanSelectionStatus status;

    @Column(name = "selected_at", nullable = false, updatable = false)
    private Instant selectedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected PlanSelection() {
    }

    private PlanSelection(UUID userId, Plan plan, Instant selectedAt, Duration ttl) {
        this.userId = requireUserId(userId);
        Plan requiredPlan = requireActivePlan(plan);
        this.planId = requirePlanId(requiredPlan);
        this.planCodeSnapshot = requireText(requiredPlan.getCode(), "planCodeSnapshot", Plan.CODE_MAX_LENGTH);
        this.planNameSnapshot = requireText(requiredPlan.getName(), "planNameSnapshot", Plan.NAME_MAX_LENGTH);
        this.planTypeSnapshot = Objects.requireNonNull(requiredPlan.getType(), "planTypeSnapshot must not be null");
        this.priceAmountSnapshot = requireNonNegative(requiredPlan.getPriceAmount(), "priceAmountSnapshot");
        this.currencySnapshot = Objects.requireNonNull(requiredPlan.getCurrency(), "currencySnapshot must not be null");
        this.durationDaysSnapshot = requirePositive(requiredPlan.getDurationDays(), "durationDaysSnapshot");
        this.trafficLimitBytesSnapshot = normalizeTrafficLimitBytes(planTypeSnapshot, requiredPlan.getTrafficLimitBytes());
        this.maxDevicesSnapshot = normalizeMaxDevices(requiredPlan.getMaxDevices());
        this.status = PlanSelectionStatus.ACTIVE;
        this.selectedAt = Objects.requireNonNull(selectedAt, "selectedAt must not be null");
        Duration positiveTtl = requirePositiveTtl(ttl);
        this.expiresAt = selectedAt.plus(positiveTtl);
        if (!expiresAt.isAfter(selectedAt)) {
            throw new IllegalArgumentException("expiresAt must be after selectedAt");
        }
    }

    public static PlanSelection create(UUID userId, Plan plan, Instant selectedAt, Duration ttl) {
        return new PlanSelection(userId, plan, selectedAt, ttl);
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return status == PlanSelectionStatus.ACTIVE && !expiresAt.isAfter(now);
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status != PlanSelectionStatus.ACTIVE || expiresAt.isAfter(now)) {
            throw new IllegalStateException("cannot expire plan selection with status " + status);
        }
        status = PlanSelectionStatus.EXPIRED;
    }

    public void clear(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status == PlanSelectionStatus.CLEARED) {
            return;
        }
        if (status != PlanSelectionStatus.ACTIVE) {
            throw new IllegalStateException("cannot clear plan selection with status " + status);
        }
        status = PlanSelectionStatus.CLEARED;
    }

    public void consume(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        if (status != PlanSelectionStatus.ACTIVE) {
            throw new IllegalStateException("cannot consume plan selection with status " + status);
        }
        status = PlanSelectionStatus.CONSUMED;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getPlanCodeSnapshot() {
        return planCodeSnapshot;
    }

    public String getPlanNameSnapshot() {
        return planNameSnapshot;
    }

    public PlanType getPlanTypeSnapshot() {
        return planTypeSnapshot;
    }

    public long getPriceAmountSnapshot() {
        return priceAmountSnapshot;
    }

    public CurrencyCode getCurrencySnapshot() {
        return currencySnapshot;
    }

    public int getDurationDaysSnapshot() {
        return durationDaysSnapshot;
    }

    public Long getTrafficLimitBytesSnapshot() {
        return trafficLimitBytesSnapshot;
    }

    public Integer getMaxDevicesSnapshot() {
        return maxDevicesSnapshot;
    }

    public PlanSelectionStatus getStatus() {
        return status;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    private static UUID requireUserId(UUID userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }

    private static Plan requireActivePlan(Plan plan) {
        Plan requiredPlan = Objects.requireNonNull(plan, "plan must not be null");
        if (requiredPlan.getStatus() != PlanStatus.ACTIVE) {
            throw new IllegalArgumentException("plan must be ACTIVE");
        }
        return requiredPlan;
    }

    private static UUID requirePlanId(Plan plan) {
        return Objects.requireNonNull(plan.getId(), "planId must not be null");
    }

    private static String requireText(String value, String fieldName, int maxLength) {
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

    private static long requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return value;
    }

    private static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static Duration requirePositiveTtl(Duration ttl) {
        Duration requiredTtl = Objects.requireNonNull(ttl, "ttl must not be null");
        if (requiredTtl.isZero() || requiredTtl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return requiredTtl;
    }

    private static Long normalizeTrafficLimitBytes(PlanType type, Long trafficLimitBytes) {
        if (type == PlanType.UNLIMITED) {
            if (trafficLimitBytes != null) {
                throw new IllegalArgumentException("trafficLimitBytesSnapshot must be null for unlimited plans");
            }
            return null;
        }
        if (trafficLimitBytes == null) {
            throw new IllegalArgumentException("trafficLimitBytesSnapshot is required for traffic-limited plans");
        }
        if (trafficLimitBytes <= 0) {
            throw new IllegalArgumentException("trafficLimitBytesSnapshot must be positive");
        }
        return trafficLimitBytes;
    }

    private static Integer normalizeMaxDevices(Integer maxDevices) {
        if (maxDevices == null) {
            return null;
        }
        if (maxDevices <= 0) {
            throw new IllegalArgumentException("maxDevicesSnapshot must be positive");
        }
        return maxDevices;
    }
}
