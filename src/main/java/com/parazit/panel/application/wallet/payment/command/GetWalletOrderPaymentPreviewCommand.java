package com.parazit.panel.application.wallet.payment.command;

import java.util.Objects;
import java.util.UUID;

public record GetWalletOrderPaymentPreviewCommand(long telegramUserId, UUID orderId) {

    public GetWalletOrderPaymentPreviewCommand {
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
