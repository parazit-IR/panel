package com.parazit.panel.application.xui.client.command;

import java.util.UUID;

public record ChangeVpnClientIpLimitCommand(
        UUID operationId,
        Long telegramUserId,
        UUID provisionId,
        int ipLimit
) {
}
