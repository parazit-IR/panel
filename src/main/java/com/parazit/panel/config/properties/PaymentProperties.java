package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        boolean enabled,
        String merchantId
) {
}
