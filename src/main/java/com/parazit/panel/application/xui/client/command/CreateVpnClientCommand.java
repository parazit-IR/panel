package com.parazit.panel.application.xui.client.command;

import java.util.Objects;
import java.util.UUID;

public record CreateVpnClientCommand(
        Long telegramUserId,
        UUID planSelectionId,
        Long inboundId
) {

    public CreateVpnClientCommand {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        Objects.requireNonNull(planSelectionId, "planSelectionId must not be null");
        if (inboundId != null && inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
    }
}
