package com.parazit.panel.application.payment.manual.command;

import java.util.UUID;

public record CancelManualCardPaymentInstructionCommand(
        UUID paymentId,
        Long telegramUserId
) {
}
