package com.parazit.panel.config.properties;

import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.renewal")
public record RenewalProperties(
        boolean enabled,
        Duration renewalOrderTtl,
        Duration renewalSelectionTtl,
        int servicePageSize,
        int planPageSize,
        boolean allowActiveSubscription,
        boolean allowExpiredSubscription,
        boolean allowSuspendedSubscription,
        RenewalTrafficPolicy defaultTrafficPolicy,
        RenewalExpiryPolicy expiryPolicy,
        boolean requireSuccessfulProvision,
        boolean requireRemoteClientReference,
        int maximumConcurrentOpenRenewalsPerSubscription
) {

    public RenewalProperties {
        renewalOrderTtl = defaultPositive(renewalOrderTtl, Duration.ofMinutes(30), "app.renewal.renewal-order-ttl");
        renewalSelectionTtl = defaultPositive(renewalSelectionTtl, Duration.ofMinutes(15), "app.renewal.renewal-selection-ttl");
        servicePageSize = positiveOrDefault(servicePageSize, 5, "app.renewal.service-page-size");
        planPageSize = positiveOrDefault(planPageSize, 5, "app.renewal.plan-page-size");
        defaultTrafficPolicy = Objects.requireNonNullElse(defaultTrafficPolicy, RenewalTrafficPolicy.RESET_TO_PLAN_LIMIT);
        expiryPolicy = Objects.requireNonNullElse(expiryPolicy, RenewalExpiryPolicy.EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY);
        if (maximumConcurrentOpenRenewalsPerSubscription <= 0) {
            maximumConcurrentOpenRenewalsPerSubscription = 1;
        }
    }

    private static Duration defaultPositive(Duration value, Duration fallback, String key) {
        Duration duration = value == null ? fallback : value;
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return duration;
    }

    private static int positiveOrDefault(int value, int fallback, String key) {
        int effective = value <= 0 ? fallback : value;
        if (effective <= 0) {
            throw new IllegalArgumentException(key + " must be positive");
        }
        return effective;
    }
}
