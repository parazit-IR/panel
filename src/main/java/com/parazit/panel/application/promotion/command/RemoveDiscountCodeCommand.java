package com.parazit.panel.application.promotion.command;

import java.util.UUID;

public record RemoveDiscountCodeCommand(
        long telegramUserId,
        UUID orderId,
        UUID requestId
) {
}
