package com.parazit.panel.application.sales;

import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PlanPurchasabilityPolicy {

    private final SalesAvailabilityService salesAvailabilityService;

    public PlanPurchasabilityPolicy(SalesAvailabilityService salesAvailabilityService) {
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
    }

    public PlanPurchasability evaluate(Plan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        if (!salesAvailabilityService.newPurchaseAvailable()) {
            return PlanPurchasability.TEMPORARILY_UNAVAILABLE;
        }
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            return PlanPurchasability.INACTIVE;
        }
        if (plan.getPriceAmount() < 0 || plan.getDurationDays() <= 0 || plan.getDisplayOrder() < 0) {
            return PlanPurchasability.INVALID_CONFIGURATION;
        }
        if (plan.getType() == PlanType.TRAFFIC_LIMITED && (plan.getTrafficLimitBytes() == null || plan.getTrafficLimitBytes() <= 0)) {
            return PlanPurchasability.INVALID_CONFIGURATION;
        }
        if (plan.getType() == PlanType.UNLIMITED && plan.getTrafficLimitBytes() != null) {
            return PlanPurchasability.INVALID_CONFIGURATION;
        }
        if (plan.getMaxDevices() != null && plan.getMaxDevices() <= 0) {
            return PlanPurchasability.INVALID_CONFIGURATION;
        }
        return PlanPurchasability.PURCHASABLE;
    }
}
