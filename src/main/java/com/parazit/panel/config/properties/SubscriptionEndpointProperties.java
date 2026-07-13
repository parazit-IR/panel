package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.subscription.endpoint")
public record SubscriptionEndpointProperties(
        String publicHost,
        Integer overridePort,
        String defaultFingerprint,
        String defaultRemarkPrefix,
        boolean preferIpv6
) {

    public SubscriptionEndpointProperties {
        publicHost = normalizeHost(publicHost);
        if (overridePort != null && overridePort == 0) {
            overridePort = null;
        }
        if (overridePort != null && (overridePort < 1 || overridePort > 65_535)) {
            throw new IllegalArgumentException("app.subscription.endpoint.override-port must be a valid TCP port");
        }
        defaultFingerprint = normalizeToken(defaultFingerprint, "chrome");
        defaultRemarkPrefix = normalizeRemarkPrefix(defaultRemarkPrefix, "Panel");
    }

    private static String normalizeHost(String value) {
        if (value == null || value.isBlank()) {
            return "localhost";
        }
        String normalized = value.trim();
        if (normalized.contains("://") || normalized.contains("/") || normalized.contains("@")) {
            throw new IllegalArgumentException("app.subscription.endpoint.public-host must be a host, not a URL");
        }
        return normalized;
    }

    private static String normalizeToken(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        if (normalized.length() > 64 || normalized.contains("\r") || normalized.contains("\n")) {
            throw new IllegalArgumentException("subscription endpoint token value is invalid");
        }
        return normalized;
    }

    private static String normalizeRemarkPrefix(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        normalized = normalized.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("app.subscription.endpoint.default-remark-prefix is too long");
        }
        return normalized;
    }
}
