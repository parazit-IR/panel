package com.parazit.panel.application.payment.manual.query;

import java.util.UUID;

public record GetManualCardPaymentInstructionQuery(
        Long telegramUserId,
        UUID paymentId
) {
}
