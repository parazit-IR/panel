package com.parazit.panel.application.xui.client.command;

import java.util.UUID;

public record AddVpnClientTrafficCommand(
        UUID operationId,
        Long telegramUserId,
        UUID provisionId,
        long additionalTrafficBytes
) {
}
