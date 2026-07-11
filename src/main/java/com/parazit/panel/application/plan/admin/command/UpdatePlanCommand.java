package com.parazit.panel.application.plan.admin.command;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import java.util.UUID;

public record UpdatePlanCommand(
        UUID planId,
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
