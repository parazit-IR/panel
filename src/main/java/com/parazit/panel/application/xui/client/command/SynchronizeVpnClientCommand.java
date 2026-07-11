package com.parazit.panel.application.xui.client.command;

import java.util.UUID;

public record SynchronizeVpnClientCommand(
        UUID operationId,
        Long telegramUserId,
        UUID provisionId
) {
}
