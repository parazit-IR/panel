package com.parazit.panel.application.renewal.command;

import java.util.UUID;

public record CreateRenewalOrderCommand(long telegramUserId, UUID purchaseSessionId, UUID requestId) {
}
