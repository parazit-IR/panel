package com.parazit.panel.application.plan.admin.query;

import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;

public record ListPlansQuery(
        PlanStatus status,
        PlanType type
) {
}
