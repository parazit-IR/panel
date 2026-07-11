package com.parazit.panel.application.xui.client.model;

import java.time.Instant;
import java.util.Objects;

public record UpdateXuiClientRequest(
        long inboundId,
        String clientId,
        String expectedEmail,
        Boolean enabled,
        Instant expiryTime,
        Long totalTrafficLimitBytes,
        Integer ipLimit,
        String newEmail
) {

    public UpdateXuiClientRequest {
        if (inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(expectedEmail, "expectedEmail must not be null");
        if (enabled == null && expiryTime == null && totalTrafficLimitBytes == null && ipLimit == null
                && (newEmail == null || newEmail.isBlank())) {
            throw new IllegalArgumentException("at least one Xui client update field must be provided");
        }
        if (totalTrafficLimitBytes != null && totalTrafficLimitBytes < 0) {
            throw new IllegalArgumentException("totalTrafficLimitBytes must be zero or positive");
        }
        if (ipLimit != null && ipLimit < 0) {
            throw new IllegalArgumentException("ipLimit must be zero or positive");
        }
    }
}
