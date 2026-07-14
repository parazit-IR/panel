package com.parazit.panel.application.wallet.topup.command;

import com.parazit.panel.domain.order.Money;
import java.util.Objects;
import java.util.UUID;

public record CreateWalletTopUpRequestCommand(
        long telegramUserId,
        Money amount,
        UUID requestId
) {

    public CreateWalletTopUpRequestCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        amount = Objects.requireNonNull(amount, "amount must not be null");
        requestId = Objects.requireNonNull(requestId, "requestId must not be null");
    }
}
