package com.parazit.panel.application.plan.catalog;

import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.domain.plan.Plan;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AvailablePlanResultMapper {

    public AvailablePlanResult toResult(Plan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        return new AvailablePlanResult(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getType(),
                plan.getPriceAmount(),
                plan.getCurrency(),
                plan.getDurationDays(),
                plan.getTrafficLimitBytes(),
                plan.getMaxDevices(),
                plan.getDisplayOrder()
        );
    }
}
