package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.customer-account")
public record CustomerAccountTelegramProperties(
        boolean enabled,
        boolean showTelegramId,
        boolean showRegistrationDate,
        boolean showServiceCounts,
        boolean showPaidOrderCount,
        boolean showPendingPaymentCount,
        boolean showUnavailableFutureFields
) {
}
