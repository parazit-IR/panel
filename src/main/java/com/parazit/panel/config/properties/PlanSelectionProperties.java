package com.parazit.panel.config.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.plan-selection")
public record PlanSelectionProperties(
        @NotNull
        Duration ttl
) {

    public PlanSelectionProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(30);
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("app.plan-selection.ttl must be positive");
        }
    }
}
