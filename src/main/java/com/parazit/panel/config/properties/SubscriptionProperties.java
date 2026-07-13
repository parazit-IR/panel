package com.parazit.panel.config.properties;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subscription")
public record SubscriptionProperties(
        boolean enabled,
        URI publicBaseUrl,
        int tokenBytes,
        int tokenPrefixLength,
        int maxTokenLength,
        Duration metadataRemoteTimeout,
        boolean includeUsageHeaders,
        boolean allowPlainFormat,
        boolean allowBase64Format,
        String defaultFormat,
        String profileTitle,
        Integer profileUpdateIntervalHours,
        URI supportUrl
) {

    public SubscriptionProperties {
        publicBaseUrl = defaultUri(publicBaseUrl, "http://localhost:8081");
        tokenBytes = tokenBytes <= 0 ? 32 : tokenBytes;
        if (tokenBytes < 32 || tokenBytes > 96) {
            throw new IllegalArgumentException("app.subscription.token-bytes must be between 32 and 96");
        }
        tokenPrefixLength = tokenPrefixLength <= 0 ? 12 : tokenPrefixLength;
        if (tokenPrefixLength < 6 || tokenPrefixLength > 20) {
            throw new IllegalArgumentException("app.subscription.token-prefix-length must be between 6 and 20");
        }
        maxTokenLength = maxTokenLength <= 0 ? 160 : maxTokenLength;
        if (maxTokenLength < 48 || maxTokenLength > 512) {
            throw new IllegalArgumentException("app.subscription.max-token-length must be between 48 and 512");
        }
        metadataRemoteTimeout = metadataRemoteTimeout == null ? Duration.ofSeconds(5) : metadataRemoteTimeout;
        if (metadataRemoteTimeout.isZero() || metadataRemoteTimeout.isNegative()) {
            throw new IllegalArgumentException("app.subscription.metadata-remote-timeout must be positive");
        }
        defaultFormat = normalizeFormat(defaultFormat);
        if (!allowPlainFormat && "plain".equals(defaultFormat)) {
            throw new IllegalArgumentException("plain default format is disabled");
        }
        if (!allowBase64Format && "base64".equals(defaultFormat)) {
            throw new IllegalArgumentException("base64 default format is disabled");
        }
        if (!allowPlainFormat && !allowBase64Format) {
            throw new IllegalArgumentException("at least one subscription format must be enabled");
        }
        profileTitle = sanitizeHeaderText(profileTitle, "Panel VPN");
        if (profileTitle.length() > 80) {
            throw new IllegalArgumentException("app.subscription.profile-title must be at most 80 characters");
        }
        if (profileUpdateIntervalHours != null && profileUpdateIntervalHours <= 0) {
            throw new IllegalArgumentException("app.subscription.profile-update-interval-hours must be positive");
        }
        validateUri(supportUrl, "supportUrl");
    }

    public URI subscriptionUrl(String rawToken) {
        String base = publicBaseUrl.toString();
        if (base.endsWith("/")) {
            return URI.create(base + "sub/" + rawToken);
        }
        return URI.create(base + "/sub/" + rawToken);
    }

    private static String normalizeFormat(String value) {
        if (value == null || value.isBlank()) {
            return "base64";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!"plain".equals(normalized) && !"base64".equals(normalized)) {
            throw new IllegalArgumentException("app.subscription.default-format must be plain or base64");
        }
        return normalized;
    }

    private static URI defaultUri(URI value, String fallback) {
        URI uri = value == null ? URI.create(fallback) : value;
        validateUri(uri, "publicBaseUrl");
        return uri;
    }

    private static void validateUri(URI uri, String fieldName) {
        if (uri == null) {
            return;
        }
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("app.subscription." + fieldName + " must be an absolute URI without credentials");
        }
    }

    private static String sanitizeHeaderText(String value, String fallback) {
        String candidate = value == null || value.isBlank() ? fallback : value.trim();
        return candidate.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
