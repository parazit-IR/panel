package com.parazit.panel.application.renewal.command;

import java.util.UUID;

public record GetRenewalPreInvoiceCommand(long telegramUserId, UUID purchaseSessionId) {
}
