package com.parazit.panel.config.properties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment.zarinpal")
public record ZarinpalProperties(
        boolean enabled,
        String merchantId,
        URI apiBaseUrl,
        URI startPayBaseUrl,
        String requestPath,
        String verifyPath,
        URI callbackBaseUrl,
        URI successRedirectUrl,
        URI failureRedirectUrl,
        URI cancelRedirectUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxTransientRetries,
        Duration retryDelay,
        boolean sandboxEnabled,
        boolean verifySsl
) {

    public ZarinpalProperties {
        merchantId = Objects.requireNonNullElse(merchantId, "");
        apiBaseUrl = defaultUri(apiBaseUrl, "https://api.zarinpal.com");
        startPayBaseUrl = defaultUri(startPayBaseUrl, "https://www.zarinpal.com/pg/StartPay");
        requestPath = normalizePath(requestPath, "/pg/v4/payment/request.json", "requestPath");
        verifyPath = normalizePath(verifyPath, "/pg/v4/payment/verify.json", "verifyPath");
        callbackBaseUrl = defaultUri(callbackBaseUrl, "http://localhost:8081/api/payments/zarinpal/callback");
        successRedirectUrl = defaultUri(successRedirectUrl, "http://localhost:8081/internal/payment-result/success");
        failureRedirectUrl = defaultUri(failureRedirectUrl, "http://localhost:8081/internal/payment-result/failed");
        cancelRedirectUrl = defaultUri(cancelRedirectUrl, "http://localhost:8081/internal/payment-result/cancelled");
        connectTimeout = defaultDuration(connectTimeout, Duration.ofSeconds(5), "connectTimeout");
        readTimeout = defaultDuration(readTimeout, Duration.ofSeconds(20), "readTimeout");
        retryDelay = defaultDuration(retryDelay, Duration.ofSeconds(1), "retryDelay");
        if (maxTransientRetries < 0 || maxTransientRetries > 5) {
            throw new IllegalArgumentException("maxTransientRetries must be between 0 and 5");
        }
        if (enabled && merchantId.isBlank()) {
            throw new IllegalArgumentException("merchantId is required when Zarinpal is enabled");
        }
    }

    public URI callbackUrl() {
        return callbackBaseUrl;
    }

    public URI startPayUrl(String authority) {
        String base = startPayBaseUrl.toString();
        if (base.endsWith("/")) {
            return URI.create(base + authority);
        }
        return URI.create(base + "/" + authority);
    }

    private static URI defaultUri(URI value, String fallback) {
        URI uri = value == null ? URI.create(fallback) : value;
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("URI must be absolute: " + uri);
        }
        return uri;
    }

    private static String normalizePath(String value, String fallback, String fieldName) {
        String path = value == null || value.isBlank() ? fallback : value.trim();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(fieldName + " must start with /");
        }
        return path;
    }

    private static Duration defaultDuration(Duration value, Duration fallback, String fieldName) {
        Duration duration = value == null ? fallback : value;
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }
}
