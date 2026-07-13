package com.parazit.panel.config.properties;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.bot")
public record TelegramBotProperties(
        boolean enabled,
        String token,
        String username,
        TelegramUpdateMode updateMode,
        Duration pollingTimeout,
        Duration pollingBackoff,
        int updateBatchSize,
        Set<String> allowedUpdates,
        int maxMessageLength,
        int maxCallbackDataBytes,
        Duration apiConnectTimeout,
        Duration apiReadTimeout,
        int maxRetryAttempts,
        Duration initialRetryDelay,
        Duration maxRetryDelay,
        double retryMultiplier,
        boolean sendQrAsPhoto,
        boolean disableLinkPreview,
        String webhookSecretToken,
        String webhookPath,
        String callbackSigningSecret,
        Duration callbackTtl
) {

    public TelegramBotProperties {
        token = normalizeSecret(token);
        username = normalizeUsername(username);
        updateMode = updateMode == null ? TelegramUpdateMode.LONG_POLLING : updateMode;
        pollingTimeout = defaultPositive(pollingTimeout, Duration.ofSeconds(30), "polling-timeout");
        pollingBackoff = defaultPositive(pollingBackoff, Duration.ofSeconds(2), "polling-backoff");
        updateBatchSize = updateBatchSize <= 0 ? 50 : updateBatchSize;
        allowedUpdates = allowedUpdates == null || allowedUpdates.isEmpty()
                ? Set.of("message", "callback_query")
                : Set.copyOf(allowedUpdates);
        maxMessageLength = maxMessageLength <= 0 ? 4096 : maxMessageLength;
        maxCallbackDataBytes = maxCallbackDataBytes <= 0 ? 64 : maxCallbackDataBytes;
        apiConnectTimeout = defaultPositive(apiConnectTimeout, Duration.ofSeconds(5), "api-connect-timeout");
        apiReadTimeout = defaultPositive(apiReadTimeout, Duration.ofSeconds(35), "api-read-timeout");
        maxRetryAttempts = maxRetryAttempts <= 0 ? 5 : maxRetryAttempts;
        initialRetryDelay = defaultPositive(initialRetryDelay, Duration.ofSeconds(1), "initial-retry-delay");
        maxRetryDelay = defaultPositive(maxRetryDelay, Duration.ofSeconds(30), "max-retry-delay");
        retryMultiplier = retryMultiplier <= 0 ? 2.0 : retryMultiplier;
        webhookSecretToken = normalizeSecret(webhookSecretToken);
        webhookPath = normalizePath(webhookPath);
        callbackSigningSecret = normalizeSecret(callbackSigningSecret);
        callbackTtl = defaultPositive(callbackTtl, Duration.ofMinutes(15), "callback-ttl");

        if (enabled && token.isBlank()) {
            throw new IllegalArgumentException("app.telegram.bot.token is required when Telegram bot is enabled");
        }
        if (enabled && callbackSigningSecret.length() < 32) {
            throw new IllegalArgumentException("app.telegram.bot.callback-signing-secret must be at least 32 characters when enabled");
        }
        if (enabled && updateMode == TelegramUpdateMode.WEBHOOK && webhookSecretToken.isBlank()) {
            throw new IllegalArgumentException("app.telegram.bot.webhook-secret-token is required for webhook mode");
        }
        if (updateBatchSize > 100) {
            throw new IllegalArgumentException("app.telegram.bot.update-batch-size must be <= 100");
        }
        if (maxMessageLength < 256 || maxMessageLength > 4096) {
            throw new IllegalArgumentException("app.telegram.bot.max-message-length must be between 256 and 4096");
        }
        if (maxCallbackDataBytes < 32 || maxCallbackDataBytes > 64) {
            throw new IllegalArgumentException("app.telegram.bot.max-callback-data-bytes must be between 32 and 64");
        }
        if (maxRetryAttempts > 10) {
            throw new IllegalArgumentException("app.telegram.bot.max-retry-attempts must be <= 10");
        }
        if (retryMultiplier < 1.0 || retryMultiplier > 5.0) {
            throw new IllegalArgumentException("app.telegram.bot.retry-multiplier must be between 1.0 and 5.0");
        }
    }

    public String botIdentity() {
        if (username != null && !username.isBlank()) {
            return username.toLowerCase(Locale.ROOT);
        }
        return "default";
    }

    @Override
    public String toString() {
        return "TelegramBotProperties[enabled=%s, username=%s, updateMode=%s, updateBatchSize=%d]"
                .formatted(enabled, username, updateMode, updateBatchSize);
    }

    private static String normalizeSecret(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/telegram/webhook";
        }
        String normalized = value.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static Duration defaultPositive(Duration value, Duration fallback, String fieldName) {
        Duration result = value == null ? fallback : value;
        if (result.isZero() || result.isNegative()) {
            throw new IllegalArgumentException("app.telegram.bot." + fieldName + " must be positive");
        }
        return result;
    }
}
