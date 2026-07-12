package com.parazit.panel.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.provisioning.outbox")
public record ProvisioningOutboxProperties(
        boolean enabled,
        Duration pollInterval,
        int batchSize,
        int maxAttempts,
        Duration initialRetryDelay,
        Duration maxRetryDelay,
        double retryMultiplier,
        Duration processingTimeout
) {

    public ProvisioningOutboxProperties {
        pollInterval = defaultDuration(pollInterval, Duration.ofSeconds(5), "pollInterval");
        batchSize = batchSize <= 0 ? 20 : batchSize;
        if (batchSize > 100) {
            throw new IllegalArgumentException("batchSize must be at most 100");
        }
        maxAttempts = maxAttempts <= 0 ? 10 : maxAttempts;
        initialRetryDelay = defaultDuration(initialRetryDelay, Duration.ofSeconds(10), "initialRetryDelay");
        maxRetryDelay = defaultDuration(maxRetryDelay, Duration.ofMinutes(15), "maxRetryDelay");
        if (maxRetryDelay.compareTo(initialRetryDelay) < 0) {
            throw new IllegalArgumentException("maxRetryDelay must be greater than or equal to initialRetryDelay");
        }
        retryMultiplier = retryMultiplier <= 1.0d ? 2.0d : retryMultiplier;
        processingTimeout = defaultDuration(processingTimeout, Duration.ofMinutes(5), "processingTimeout");
    }

    private static Duration defaultDuration(Duration value, Duration fallback, String fieldName) {
        Duration duration = value == null ? fallback : value;
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }
}
