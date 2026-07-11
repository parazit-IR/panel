package com.parazit.panel.application.xui.client.command;

import java.util.Objects;
import java.util.UUID;

public record DisableVpnClientCommand(
        Long telegramUserId,
        UUID provisionId
) {

    public DisableVpnClientCommand {
        if (telegramUserId == null || telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        Objects.requireNonNull(provisionId, "provisionId must not be null");
    }
}
