package com.parazit.panel.application.wallet.payment.command;

import java.util.Objects;
import java.util.UUID;

public record ReconcileWalletOrderPaymentCommand(UUID orderId) {

    public ReconcileWalletOrderPaymentCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
