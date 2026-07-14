package com.parazit.panel.config.properties;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wallet.payment")
public record WalletPaymentProperties(
        boolean enabled,
        boolean allowNewSubscription,
        boolean allowRenewal,
        CurrencyCode currency,
        long minimumPurchaseAmount,
        long maximumPurchaseAmount,
        int maxConcurrentAttemptsPerUser,
        Duration paymentTtl
) {

    public WalletPaymentProperties {
        currency = Objects.requireNonNullElse(currency, CurrencyCode.IRT);
        if (minimumPurchaseAmount < 0) {
            minimumPurchaseAmount = 0;
        }
        if (maximumPurchaseAmount < 0) {
            maximumPurchaseAmount = 0;
        }
        if (maximumPurchaseAmount > 0 && maximumPurchaseAmount < minimumPurchaseAmount) {
            throw new IllegalArgumentException("maximumPurchaseAmount must be greater than or equal to minimumPurchaseAmount");
        }
        if (maxConcurrentAttemptsPerUser <= 0) {
            maxConcurrentAttemptsPerUser = 3;
        }
        paymentTtl = paymentTtl == null ? Duration.ofMinutes(15) : paymentTtl;
        if (paymentTtl.isZero() || paymentTtl.isNegative()) {
            throw new IllegalArgumentException("paymentTtl must be positive");
        }
    }

    public Money minimumPurchaseMoney() {
        return new Money(minimumPurchaseAmount, currency);
    }

    public Money maximumPurchaseMoney() {
        return maximumPurchaseAmount <= 0 ? null : new Money(maximumPurchaseAmount, currency);
    }
}
