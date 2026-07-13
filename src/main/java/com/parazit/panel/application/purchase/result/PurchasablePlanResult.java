package com.parazit.panel.application.purchase.result;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public record PurchasablePlanResult(
        UUID planId,
        String code,
        String name,
        String description,
        long priceAmount,
        CurrencyCode currency,
        int durationDays,
        OptionalLong trafficBytes,
        OptionalInt maxDevices,
        PlanType planType,
        boolean active,
        boolean visible,
        boolean purchasable,
        String unavailabilityCode,
        Instant versionTimestamp
) {
}
