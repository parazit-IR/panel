package com.parazit.panel.application.wallet.payment.command;

import java.util.Objects;
import java.util.UUID;

public record PayOrderWithWalletCommand(
        long telegramUserId,
        UUID orderId,
        UUID requestId
) {

    public PayOrderWithWalletCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(requestId, "requestId must not be null");
    }
}
