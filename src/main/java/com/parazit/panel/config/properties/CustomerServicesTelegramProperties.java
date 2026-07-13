package com.parazit.panel.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.customer-services")
public record CustomerServicesTelegramProperties(
        boolean enabled,
        int pageSize,
        int maxPageSize,
        int searchMinimumLength,
        int searchMaximumLength,
        int searchResultLimit,
        Duration searchConversationTtl,
        Duration usageFreshnessTtl,
        Duration minimumRefreshInterval,
        boolean showUsage,
        boolean showExpiredServices,
        boolean showSuspendedServices,
        boolean showRevokedServices,
        boolean allowVlessDelivery,
        boolean allowQrDelivery,
        boolean allowSubscriptionDelivery
) {

    public CustomerServicesTelegramProperties {
        pageSize = pageSize <= 0 ? 5 : pageSize;
        maxPageSize = maxPageSize <= 0 ? 10 : maxPageSize;
        searchMinimumLength = searchMinimumLength <= 0 ? 3 : searchMinimumLength;
        searchMaximumLength = searchMaximumLength <= 0 ? 64 : searchMaximumLength;
        searchResultLimit = searchResultLimit <= 0 ? 10 : searchResultLimit;
        searchConversationTtl = searchConversationTtl == null ? Duration.ofMinutes(5) : searchConversationTtl;
        usageFreshnessTtl = usageFreshnessTtl == null ? Duration.ofMinutes(30) : usageFreshnessTtl;
        minimumRefreshInterval = minimumRefreshInterval == null ? Duration.ofSeconds(30) : minimumRefreshInterval;
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("app.telegram.customer-services.page-size must be <= max-page-size");
        }
        if (maxPageSize > 20) {
            throw new IllegalArgumentException("app.telegram.customer-services.max-page-size must be <= 20");
        }
        if (searchMinimumLength > searchMaximumLength) {
            throw new IllegalArgumentException("app.telegram.customer-services search minimum must be <= maximum");
        }
        if (searchMaximumLength > 128) {
            throw new IllegalArgumentException("app.telegram.customer-services.search-maximum-length must be <= 128");
        }
        if (searchResultLimit > 20) {
            throw new IllegalArgumentException("app.telegram.customer-services.search-result-limit must be <= 20");
        }
        if (!searchConversationTtl.isPositive() || !usageFreshnessTtl.isPositive() || !minimumRefreshInterval.isPositive()) {
            throw new IllegalArgumentException("app.telegram.customer-services durations must be positive");
        }
        if (enabled && !allowVlessDelivery && !allowQrDelivery && !allowSubscriptionDelivery) {
            throw new IllegalArgumentException("at least one customer service delivery method must be enabled");
        }
    }
}
