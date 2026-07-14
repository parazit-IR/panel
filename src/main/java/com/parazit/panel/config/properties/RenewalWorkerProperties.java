package com.parazit.panel.config.properties;

import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.renewal.worker")
public record RenewalWorkerProperties(
        boolean enabled,
        Duration pollInterval,
        int batchSize,
        Duration lockTimeout,
        Duration remoteTimeout,
        int maxAttempts,
        Duration initialBackoff,
        Duration maxBackoff,
        double backoffMultiplier,
        String workerId,
        boolean processOnStartup
) {

    public RenewalWorkerProperties {
        pollInterval = positive(pollInterval, Duration.ofSeconds(5), "app.renewal.worker.poll-interval");
        batchSize = batchSize <= 0 ? 10 : batchSize;
        if (batchSize > 100) {
            throw new IllegalArgumentException("app.renewal.worker.batch-size must be at most 100");
        }
        lockTimeout = positive(lockTimeout, Duration.ofMinutes(2), "app.renewal.worker.lock-timeout");
        remoteTimeout = positive(remoteTimeout, Duration.ofSeconds(20), "app.renewal.worker.remote-timeout");
        maxAttempts = maxAttempts <= 0 ? 8 : maxAttempts;
        initialBackoff = positive(initialBackoff, Duration.ofSeconds(10), "app.renewal.worker.initial-backoff");
        maxBackoff = positive(maxBackoff, Duration.ofMinutes(30), "app.renewal.worker.max-backoff");
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("app.renewal.worker.max-backoff must be greater than or equal to initial-backoff");
        }
        backoffMultiplier = backoffMultiplier <= 1.0d ? 2.0d : backoffMultiplier;
        workerId = workerId == null || workerId.isBlank()
                ? "renewal-worker-" + UUID.randomUUID()
                : workerId.trim();
        if (workerId.length() > 128) {
            workerId = workerId.substring(0, 128);
        }
    }

    private static Duration positive(Duration value, Duration fallback, String key) {
        Duration duration = value == null ? fallback : value;
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return duration;
    }
}
