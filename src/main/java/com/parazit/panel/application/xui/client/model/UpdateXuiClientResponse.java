package com.parazit.panel.application.xui.client.model;

import java.time.Instant;

public record UpdateXuiClientResponse(
        long inboundId,
        String clientId,
        String email,
        boolean enabled,
        Instant expiryTime,
        long totalTrafficLimitBytes,
        int ipLimit,
        boolean updated,
        String remoteMessage
) {
}
