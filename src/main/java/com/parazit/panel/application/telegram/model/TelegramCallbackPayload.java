package com.parazit.panel.application.telegram.model;

import java.time.Instant;
import java.util.UUID;

public record TelegramCallbackPayload(
        TelegramCallbackAction action,
        UUID subscriptionId,
        Integer configIndex,
        UUID actionId,
        String reference,
        Instant expiresAt
) {

    public TelegramCallbackPayload {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (configIndex != null && configIndex < 1) {
            throw new IllegalArgumentException("configIndex must be positive");
        }
        reference = reference == null ? "" : reference.trim();
        if (reference.length() > 32) {
            throw new IllegalArgumentException("callback reference is too long");
        }
        if (!reference.isBlank() && !reference.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("callback reference contains unsupported characters");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }

    public TelegramCallbackPayload(
            TelegramCallbackAction action,
            UUID subscriptionId,
            Integer configIndex,
            UUID actionId,
            Instant expiresAt
    ) {
        this(action, subscriptionId, configIndex, actionId, "", expiresAt);
    }
}
