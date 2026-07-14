package com.parazit.panel.application.wallet.topup.command;

import java.util.Objects;
import java.util.UUID;

public record GetWalletTopUpStatusCommand(long telegramUserId, UUID topUpRequestId) {

    public GetWalletTopUpStatusCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        topUpRequestId = Objects.requireNonNull(topUpRequestId, "topUpRequestId must not be null");
    }
}
