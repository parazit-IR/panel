package com.parazit.panel.config.properties;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        boolean enabled,
        String merchantId,
        Duration defaultExpiration
) {

    private static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(30);

    public PaymentProperties {
        defaultExpiration = defaultExpiration == null ? DEFAULT_EXPIRATION : defaultExpiration;
        if (defaultExpiration.isZero() || defaultExpiration.isNegative()) {
            throw new IllegalArgumentException("defaultExpiration must be positive");
        }
        merchantId = Objects.requireNonNullElse(merchantId, "");
    }
}
