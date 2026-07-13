package com.parazit.panel.application.customer.result;

import java.util.List;

public record CustomerServicePageResult(
        List<CustomerServiceSummaryResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public CustomerServicePageResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("totals must be non-negative");
        }
    }
}
