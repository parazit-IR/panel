package com.parazit.panel.application.payment.zarinpal.command;

import java.util.UUID;

public record InitializeZarinpalPaymentCommand(
        UUID requestId,
        UUID paymentId,
        Long telegramUserId,
        String description,
        String mobile,
        String email
) {
}
