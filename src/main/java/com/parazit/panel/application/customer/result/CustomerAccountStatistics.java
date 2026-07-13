package com.parazit.panel.application.customer.result;

public record CustomerAccountStatistics(
        long totalServiceCount,
        long activeServiceCount,
        long expiredServiceCount,
        long paidOrderCount,
        long pendingPaymentCount
) {

    public CustomerAccountStatistics {
        requireNonNegative(totalServiceCount, "totalServiceCount");
        requireNonNegative(activeServiceCount, "activeServiceCount");
        requireNonNegative(expiredServiceCount, "expiredServiceCount");
        requireNonNegative(paidOrderCount, "paidOrderCount");
        requireNonNegative(pendingPaymentCount, "pendingPaymentCount");
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
    }
}
