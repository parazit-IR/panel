package com.parazit.panel.config.properties;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wallet.top-up")
public record WalletTopUpProperties(
        boolean enabled,
        CurrencyCode currency,
        long minimumAmount,
        long maximumAmount,
        Duration requestTtl,
        int maxPendingRequestsPerUser,
        boolean manualPaymentEnabled,
        boolean onlinePaymentEnabled
) {

    public WalletTopUpProperties {
        currency = Objects.requireNonNullElse(currency, CurrencyCode.IRT);
        if (minimumAmount <= 0) {
            minimumAmount = 1_000L;
        }
        if (maximumAmount <= 0) {
            maximumAmount = 10_000_000L;
        }
        if (maximumAmount < minimumAmount) {
            throw new IllegalArgumentException("maximumAmount must be greater than or equal to minimumAmount");
        }
        requestTtl = requestTtl == null ? Duration.ofMinutes(30) : requestTtl;
        if (requestTtl.isZero() || requestTtl.isNegative()) {
            throw new IllegalArgumentException("requestTtl must be positive");
        }
        if (maxPendingRequestsPerUser <= 0) {
            maxPendingRequestsPerUser = 3;
        }
    }

    public Money minimumMoney() {
        return new Money(minimumAmount, currency);
    }

    public Money maximumMoney() {
        return new Money(maximumAmount, currency);
    }
}
