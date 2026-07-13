package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.feature-placeholder")
public record TelegramFeaturePlaceholderProperties(
        boolean renewalAvailable,
        boolean trialAvailable,
        boolean walletAvailable,
        boolean tutorialsAvailable,
        boolean supportAvailable
) {
}
