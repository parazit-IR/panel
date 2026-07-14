package com.parazit.panel.application.promotion.result;

import com.parazit.panel.domain.order.Money;
import java.time.Instant;
import java.util.UUID;

public record DiscountApplicationResult(
        UUID orderId,
        UUID redemptionId,
        Money originalAmount,
        Money discountAmount,
        Money finalAmount,
        boolean replayed,
        Instant appliedAt
) {
}
