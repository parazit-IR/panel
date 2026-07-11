package com.parazit.panel.application.xui.model;

import java.time.Instant;

public record XuiClientSnapshot(
        String clientId,
        String email,
        boolean enabled,
        long totalTrafficLimitBytes,
        long uploadBytes,
        long downloadBytes,
        Instant expiryTime,
        int ipLimit,
        String subscriptionId
) {
}
