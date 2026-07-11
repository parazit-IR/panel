package com.parazit.panel.api.internal.plan.admin;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdatePlanRequest(
        @NotBlank
        @Size(max = 128)
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        PlanType type,

        @NotNull
        @PositiveOrZero
        Long priceAmount,

        @NotNull
        CurrencyCode currency,

        @NotNull
        @Positive
        Integer durationDays,

        Long trafficLimitBytes,

        @Positive
        Integer maxDevices,

        @NotNull
        @PositiveOrZero
        Integer displayOrder
) {
}
