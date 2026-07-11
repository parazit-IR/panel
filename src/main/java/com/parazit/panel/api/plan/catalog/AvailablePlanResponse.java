package com.parazit.panel.api.plan.catalog;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import java.util.UUID;

public record AvailablePlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        PlanType type,
        long priceAmount,
        CurrencyCode currency,
        int durationDays,
        Long trafficLimitBytes,
        Integer maxDevices
) {
}
