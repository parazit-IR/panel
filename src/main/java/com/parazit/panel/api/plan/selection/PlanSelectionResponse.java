package com.parazit.panel.api.plan.selection;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import java.time.Instant;
import java.util.UUID;

public record PlanSelectionResponse(
        UUID selectionId,
        UUID planId,
        String planCode,
        String planName,
        PlanType planType,
        long priceAmount,
        CurrencyCode currency,
        int durationDays,
        Long trafficLimitBytes,
        Integer maxDevices,
        PlanSelectionStatus status,
        Instant selectedAt,
        Instant expiresAt,
        boolean newlySelected
) {
}
