package com.parazit.panel.application.plan.selection.result;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import java.time.Instant;
import java.util.UUID;

public record PlanSelectionResult(
        UUID selectionId,
        UUID userId,
        Long telegramUserId,
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
