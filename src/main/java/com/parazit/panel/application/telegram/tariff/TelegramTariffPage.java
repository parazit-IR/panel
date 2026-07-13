package com.parazit.panel.application.telegram.tariff;

import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import java.util.List;

public record TelegramTariffPage(
        List<AvailablePlanResult> plans,
        int page,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {

    public TelegramTariffPage {
        plans = plans == null ? List.of() : List.copyOf(plans);
        page = Math.max(1, page);
        totalPages = Math.max(1, totalPages);
    }
}
