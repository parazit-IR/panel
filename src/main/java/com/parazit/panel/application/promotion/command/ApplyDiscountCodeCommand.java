package com.parazit.panel.application.promotion.command;

import java.util.UUID;

public record ApplyDiscountCodeCommand(
        long telegramUserId,
        UUID orderId,
        String rawCode,
        UUID requestId
) {
}
