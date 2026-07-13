package com.parazit.panel.application.telegram.model;

import java.time.Instant;
import java.util.UUID;

public record TelegramCallbackPayload(
        TelegramCallbackAction action,
        UUID subscriptionId,
        Integer configIndex,
        UUID actionId,
        Instant expiresAt
) {

    public TelegramCallbackPayload {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (configIndex != null && configIndex < 1) {
            throw new IllegalArgumentException("configIndex must be positive");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }
}
