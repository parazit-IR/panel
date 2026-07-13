package com.parazit.panel.application.renewal.result;

import java.util.List;

public record RenewalPlanPageResult(
        List<RenewalPlanSummaryResult> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
