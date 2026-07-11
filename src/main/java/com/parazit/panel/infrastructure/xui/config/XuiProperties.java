package com.parazit.panel.infrastructure.xui.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.xui")
public record XuiProperties(
        @NotBlank
        String baseUrl,
        String username,
        String password,
        @NotNull
        Duration connectTimeout,
        @NotNull
        Duration readTimeout,
        Duration loginTimeout,
        @Min(0)
        int maxRetries,
        @NotNull
        Duration retryDelay,
        boolean verifySsl,
        Boolean autoLogin,
        Duration sessionTimeout
) {

    public XuiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:2053";
        }
        baseUrl = normalizeBaseUrl(baseUrl);
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(20);
        }
        if (loginTimeout == null) {
            loginTimeout = Duration.ofSeconds(10);
        }
        if (retryDelay == null) {
            retryDelay = Duration.ofSeconds(1);
        }
        if (autoLogin == null) {
            autoLogin = true;
        }
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("app.xui.connect-timeout must be positive");
        }
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("app.xui.read-timeout must be positive");
        }
        if (loginTimeout.isZero() || loginTimeout.isNegative()) {
            throw new IllegalArgumentException("app.xui.login-timeout must be positive");
        }
        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException("app.xui.retry-delay must be zero or positive");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("app.xui.max-retries must be zero or positive");
        }
        if (sessionTimeout != null && (sessionTimeout.isZero() || sessionTimeout.isNegative())) {
            throw new IllegalArgumentException("app.xui.session-timeout must be positive when configured");
        }
    }

    private static String normalizeBaseUrl(String value) {
        URI uri = URI.create(value.trim());
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("app.xui.base-url must be an absolute URL");
        }
        String normalized = uri.toString();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
