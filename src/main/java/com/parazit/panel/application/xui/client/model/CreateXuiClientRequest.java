package com.parazit.panel.application.xui.client.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreateXuiClientRequest(
        long inboundId,
        String clientId,
        String email,
        String subscriptionId,
        boolean enabled,
        long totalTrafficLimitBytes,
        Instant expiryTime,
        int ipLimit,
        String flow
) {

    public CreateXuiClientRequest {
        if (inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
        UUID.fromString(Objects.requireNonNull(clientId, "clientId must not be null"));
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        email = email.trim();
        if (totalTrafficLimitBytes < 0) {
            throw new IllegalArgumentException("totalTrafficLimitBytes must be zero or positive");
        }
        Objects.requireNonNull(expiryTime, "expiryTime must not be null");
        if (ipLimit < 0) {
            throw new IllegalArgumentException("ipLimit must be zero or positive");
        }
        flow = flow == null ? "" : flow.trim();
    }
}
