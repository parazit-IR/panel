package com.parazit.panel.application.wallet.topup.command;

import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.Objects;
import java.util.UUID;

public record CreateWalletTopUpPaymentCommand(
        long telegramUserId,
        UUID topUpRequestId,
        PaymentMethod paymentMethod,
        UUID requestId
) {

    public CreateWalletTopUpPaymentCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        topUpRequestId = Objects.requireNonNull(topUpRequestId, "topUpRequestId must not be null");
        paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod must not be null");
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
    }
}
