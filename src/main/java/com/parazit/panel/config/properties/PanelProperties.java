package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.panel")
public record PanelProperties(
        boolean enabled,
        String url,
        String username,
        String password
) {
}
