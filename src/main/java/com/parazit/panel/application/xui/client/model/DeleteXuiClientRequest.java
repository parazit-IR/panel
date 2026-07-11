package com.parazit.panel.application.xui.client.model;

import java.util.Objects;
import java.util.UUID;

public record DeleteXuiClientRequest(
        long inboundId,
        String clientId,
        String email
) {

    public DeleteXuiClientRequest {
        if (inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
        clientId = requireUuid(clientId, "clientId");
        email = requireText(email, "email");
    }

    private static String requireUuid(String value, String fieldName) {
        String normalized = requireText(value, fieldName);
        UUID.fromString(normalized);
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
