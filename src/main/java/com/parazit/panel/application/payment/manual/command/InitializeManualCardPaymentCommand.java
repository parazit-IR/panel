package com.parazit.panel.application.payment.manual.command;

import java.util.UUID;

public record InitializeManualCardPaymentCommand(
        UUID instructionRequestId,
        UUID paymentId,
        Long telegramUserId
) {
}
