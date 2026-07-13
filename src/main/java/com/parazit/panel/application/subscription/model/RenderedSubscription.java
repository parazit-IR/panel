package com.parazit.panel.application.subscription.model;

import java.util.Arrays;

public record RenderedSubscription(
        byte[] body,
        String contentType,
        SubscriptionResponseHeaders headers
) {

    public RenderedSubscription {
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("body must not be empty");
        }
        body = Arrays.copyOf(body, body.length);
        contentType = contentType == null || contentType.isBlank() ? "text/plain; charset=utf-8" : contentType;
        headers = headers == null ? new SubscriptionResponseHeaders(null) : headers;
    }

    @Override
    public byte[] body() {
        return Arrays.copyOf(body, body.length);
    }
}
