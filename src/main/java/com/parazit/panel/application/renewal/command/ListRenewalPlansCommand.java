package com.parazit.panel.application.renewal.command;

import java.util.UUID;

public record ListRenewalPlansCommand(long telegramUserId, UUID subscriptionId, int page, int size) {
}
