package com.parazit.panel.application.renewal.result;

import java.util.List;

public record RenewableServicePageResult(
        List<RenewableServiceSummaryResult> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
