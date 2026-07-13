package com.parazit.panel.application.renewal.command;

import java.util.UUID;

public record SelectRenewalPlanCommand(long telegramUserId, UUID subscriptionId, UUID planId, UUID requestId) {
}
