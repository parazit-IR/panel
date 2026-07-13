package com.parazit.panel.application.subscription.delivery;

import java.util.Objects;
import java.util.UUID;

public final class SubscriptionRenderedContentResult {

    private final UUID subscriptionId;
    private final String format;
    private final String contentType;
    private final byte[] body;

    public SubscriptionRenderedContentResult(UUID subscriptionId, String format, String contentType, byte[] body) {
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        this.format = requireText(format, "format");
        this.contentType = requireText(contentType, "contentType");
        this.body = Objects.requireNonNull(body, "body must not be null").clone();
    }

    public UUID subscriptionId() {
        return subscriptionId;
    }

    public String format() {
        return format;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] body() {
        return body.clone();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

