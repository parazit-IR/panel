package com.parazit.panel.application.promotion.command;

import java.util.UUID;

public record RedeemGiftCodeCommand(
        long telegramUserId,
        String rawCode,
        UUID requestId
) {
}
