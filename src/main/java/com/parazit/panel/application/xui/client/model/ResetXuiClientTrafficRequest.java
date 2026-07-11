package com.parazit.panel.application.xui.client.model;

import java.util.Objects;

public record ResetXuiClientTrafficRequest(
        long inboundId,
        String clientId,
        String email
) {

    public ResetXuiClientTrafficRequest {
        if (inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(email, "email must not be null");
    }
}
