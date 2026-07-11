package com.parazit.panel.application.xui.client.command;

import java.util.UUID;

public record EnableVpnClientCommand(
        UUID operationId,
        Long telegramUserId,
        UUID provisionId
) {
}
