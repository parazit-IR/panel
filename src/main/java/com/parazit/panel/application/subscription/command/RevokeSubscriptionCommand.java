package com.parazit.panel.application.subscription.command;

import java.util.UUID;

public record RevokeSubscriptionCommand(
        Long telegramUserId,
        UUID subscriptionId,
        String reason
) {
}
