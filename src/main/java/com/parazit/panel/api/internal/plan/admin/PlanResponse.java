package com.parazit.panel.api.internal.plan.admin;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import java.time.Instant;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        PlanStatus status,
        PlanType type,
        long priceAmount,
        CurrencyCode currency,
        int durationDays,
        Long trafficLimitBytes,
        Integer maxDevices,
        int displayOrder,
        boolean available,
        Instant createdAt,
        Instant updatedAt
) {
}
