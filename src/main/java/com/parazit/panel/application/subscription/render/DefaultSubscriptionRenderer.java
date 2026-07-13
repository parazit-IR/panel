package com.parazit.panel.application.subscription.render;

import com.parazit.panel.application.subscription.model.RenderedSubscription;
import com.parazit.panel.application.subscription.model.SubscriptionContent;
import com.parazit.panel.application.subscription.model.SubscriptionConfigEntry;
import com.parazit.panel.application.subscription.model.SubscriptionResponseHeaders;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DefaultSubscriptionRenderer implements SubscriptionRenderer {

    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    private final VlessUriBuilder vlessUriBuilder;

    public DefaultSubscriptionRenderer(VlessUriBuilder vlessUriBuilder) {
        this.vlessUriBuilder = Objects.requireNonNull(vlessUriBuilder, "vlessUriBuilder must not be null");
    }

    @Override
    public RenderedSubscription renderPlain(SubscriptionContent content) {
        byte[] body = plainText(content).getBytes(StandardCharsets.UTF_8);
        return new RenderedSubscription(body, CONTENT_TYPE, headers(content));
    }

    @Override
    public RenderedSubscription renderBase64(SubscriptionContent content) {
        byte[] plain = plainText(content).getBytes(StandardCharsets.UTF_8);
        byte[] encoded = Base64.getEncoder().encode(plain);
        return new RenderedSubscription(encoded, CONTENT_TYPE, headers(content));
    }

    private String plainText(SubscriptionContent content) {
        Objects.requireNonNull(content, "content must not be null");
        StringBuilder builder = new StringBuilder();
        for (SubscriptionConfigEntry entry : content.entries()) {
            builder.append(vlessUriBuilder.build(entry.vless())).append('\n');
        }
        return builder.toString();
    }

    private SubscriptionResponseHeaders headers(SubscriptionContent content) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Cache-Control", "no-store, no-cache, must-revalidate, private");
        values.put("Pragma", "no-cache");
        values.put("Expires", "0");
        values.put("X-Content-Type-Options", "nosniff");
        values.put("Content-Disposition", "inline");
        if (content.uploadBytes() != null && content.downloadBytes() != null && content.totalBytes() != null) {
            StringBuilder userInfo = new StringBuilder()
                    .append("upload=").append(content.uploadBytes())
                    .append("; download=").append(content.downloadBytes())
                    .append("; total=").append(content.totalBytes());
            Instant expiresAt = content.expiresAt();
            if (expiresAt != null) {
                userInfo.append("; expire=").append(expiresAt.getEpochSecond());
            }
            values.put("subscription-userinfo", userInfo.toString());
        }
        values.put("profile-title", "base64:" + Base64.getEncoder().encodeToString(content.title().getBytes(StandardCharsets.UTF_8)));
        if (content.profileUpdateInterval() != null && !content.profileUpdateInterval().isBlank()) {
            values.put("profile-update-interval", content.profileUpdateInterval());
        }
        if (content.supportUrl() != null && !content.supportUrl().isBlank()) {
            values.put("support-url", content.supportUrl());
        }
        return new SubscriptionResponseHeaders(values);
    }
}
