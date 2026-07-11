package com.parazit.panel.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.referral")
public record ReferralProperties(
        boolean enabled,
        @Min(8)
        @Max(16)
        int codeLength
) {
}
