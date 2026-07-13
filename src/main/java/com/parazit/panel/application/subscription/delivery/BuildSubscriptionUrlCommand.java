package com.parazit.panel.application.subscription.delivery;

import java.util.UUID;

public record BuildSubscriptionUrlCommand(
        Long telegramUserId,
        UUID subscriptionId,
        String rawAccessToken
) {
}

