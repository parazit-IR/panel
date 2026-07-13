package com.parazit.panel.application.subscription.command;

import java.util.UUID;

public record CreateSubscriptionCommand(
        Long telegramUserId,
        UUID xuiClientProvisionId
) {
}
