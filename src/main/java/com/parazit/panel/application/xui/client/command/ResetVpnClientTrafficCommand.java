package com.parazit.panel.application.xui.client.command;

import java.util.UUID;

public record ResetVpnClientTrafficCommand(
        UUID operationId,
        Long telegramUserId,
        UUID provisionId
) {
}
