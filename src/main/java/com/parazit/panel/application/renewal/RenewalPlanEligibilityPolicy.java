package com.parazit.panel.application.renewal;

import com.parazit.panel.application.sales.SalesAvailabilityService;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalPlanEligibilityPolicy {

    private final SalesAvailabilityService salesAvailabilityService;

    public RenewalPlanEligibilityPolicy(SalesAvailabilityService salesAvailabilityService) {
        this.salesAvailabilityService = Objects.requireNonNull(salesAvailabilityService, "salesAvailabilityService must not be null");
    }

    public boolean eligible(Plan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        if (!salesAvailabilityService.availability(com.parazit.panel.application.sales.SalesCapability.RENEWAL).enabled()) {
            return false;
        }
        if (plan.getStatus() != PlanStatus.ACTIVE || !plan.isRenewalEnabled()) {
            return false;
        }
        if (plan.getPriceAmount() < 0 || plan.getDurationDays() <= 0) {
            return false;
        }
        if (plan.getType() == PlanType.TRAFFIC_LIMITED && (plan.getTrafficLimitBytes() == null || plan.getTrafficLimitBytes() <= 0)) {
            return false;
        }
        if (plan.getType() == PlanType.UNLIMITED && plan.getTrafficLimitBytes() != null) {
            return false;
        }
        return plan.getMaxDevices() == null || plan.getMaxDevices() > 0;
    }
}
