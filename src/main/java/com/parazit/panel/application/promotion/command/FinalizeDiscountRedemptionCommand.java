package com.parazit.panel.application.promotion.command;

import java.util.UUID;

public record FinalizeDiscountRedemptionCommand(
        UUID orderId,
        UUID paymentId,
        UUID requestId
) {
}
