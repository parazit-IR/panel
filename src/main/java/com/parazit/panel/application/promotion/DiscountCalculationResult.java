package com.parazit.panel.application.promotion;

import com.parazit.panel.domain.order.Money;

public record DiscountCalculationResult(
        Money originalAmount,
        Money discountAmount,
        Money finalAmount
) {
}
