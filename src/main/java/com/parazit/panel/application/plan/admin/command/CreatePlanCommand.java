package com.parazit.panel.application.plan.admin.command;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;

public record CreatePlanCommand(
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
}
