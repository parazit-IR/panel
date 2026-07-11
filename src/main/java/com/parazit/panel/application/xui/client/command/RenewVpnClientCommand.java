package com.parazit.panel.application.xui.client.command;

import com.parazit.panel.application.xui.client.model.RenewalMode;
import java.util.UUID;

public record RenewVpnClientCommand(
        UUID operationId,
        Long telegramUserId,
        UUID provisionId,
        int durationDays,
        RenewalMode renewalMode
) {
}
