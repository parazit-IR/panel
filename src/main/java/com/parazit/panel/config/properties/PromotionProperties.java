package com.parazit.panel.config.properties;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.promotion")
public record PromotionProperties(
        boolean discountEnabled,
        boolean giftEnabled,
        int codeMinLength,
        int codeMaxLength,
        Duration reservationTtl,
        boolean allowDiscountOnNewSubscription,
        boolean allowDiscountOnRenewal,
        boolean allowZeroFinalAmount,
        boolean allowDiscountStacking,
        String hashSecret
) {

    public PromotionProperties {
        if (codeMinLength <= 0) {
            codeMinLength = 4;
        }
        if (codeMaxLength <= 0) {
            codeMaxLength = 32;
        }
        if (codeMinLength > codeMaxLength) {
            throw new IllegalArgumentException("codeMinLength must be <= codeMaxLength");
        }
        reservationTtl = reservationTtl == null ? Duration.ofMinutes(30) : reservationTtl;
        if (reservationTtl.isZero() || reservationTtl.isNegative()) {
            throw new IllegalArgumentException("reservationTtl must be positive");
        }
        hashSecret = Objects.requireNonNullElse(hashSecret, "").trim();
    }
}
