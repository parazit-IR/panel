package com.parazit.panel.application.xui.client.command;

import java.util.Objects;
import java.util.UUID;

public record DeleteVpnClientCommand(
        Long telegramUserId,
        UUID provisionId,
        boolean force
) {

    public DeleteVpnClientCommand {
        if (telegramUserId == null || telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        Objects.requireNonNull(provisionId, "provisionId must not be null");
    }
}
