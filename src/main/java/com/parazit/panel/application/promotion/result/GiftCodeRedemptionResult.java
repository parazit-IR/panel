package com.parazit.panel.application.promotion.result;

import com.parazit.panel.domain.order.Money;
import java.time.Instant;
import java.util.UUID;

public record GiftCodeRedemptionResult(
        UUID redemptionId,
        Money creditedAmount,
        Money balanceBefore,
        Money balanceAfter,
        GiftCodeRedemptionOutcome outcome,
        boolean replayed,
        Instant redeemedAt
) {
}
