package com.parazit.panel.application.renewal.command;

import java.util.UUID;

public record GetRenewalTargetDetailsCommand(long telegramUserId, UUID subscriptionId) {
}
