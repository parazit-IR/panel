package com.parazit.panel.application.wallet.command;

import java.util.Objects;
import java.util.UUID;

public record ReconcileWalletBalanceCommand(UUID walletId) {

    public ReconcileWalletBalanceCommand {
        walletId = Objects.requireNonNull(walletId, "walletId must not be null");
    }
}
