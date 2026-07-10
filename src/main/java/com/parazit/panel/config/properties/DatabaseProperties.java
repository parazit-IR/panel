package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.database")
public record DatabaseProperties(
        String url,
        String username,
        String password
) {
}
