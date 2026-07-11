package com.parazit.panel.application.plan;

import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PlanResultMapper {

    public PlanResult toResult(Plan plan) {
        Objects.requireNonNull(plan, "plan must not be null");
        return new PlanResult(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getStatus(),
                plan.getType(),
                plan.getPriceAmount(),
                plan.getCurrency(),
                plan.getDurationDays(),
                plan.getTrafficLimitBytes(),
                plan.getMaxDevices(),
                plan.getDisplayOrder(),
                plan.getStatus() == PlanStatus.ACTIVE,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
