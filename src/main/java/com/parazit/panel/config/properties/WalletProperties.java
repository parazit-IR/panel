package com.parazit.panel.config.properties;

import com.parazit.panel.domain.plan.CurrencyCode;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wallet")
public record WalletProperties(
        boolean enabled,
        CurrencyCode currency,
        int historyPageSize,
        int maxHistoryPageSize,
        boolean autoCreateOnUserRegistration,
        boolean showTransactionHistory,
        boolean showTopUpPlaceholder,
        int optimisticRetryCount
) {

    public WalletProperties {
        currency = Objects.requireNonNullElse(currency, CurrencyCode.IRT);
        historyPageSize = historyPageSize <= 0 ? 10 : historyPageSize;
        maxHistoryPageSize = maxHistoryPageSize <= 0 ? 50 : maxHistoryPageSize;
        if (historyPageSize > maxHistoryPageSize) {
            historyPageSize = maxHistoryPageSize;
        }
        optimisticRetryCount = optimisticRetryCount <= 0 ? 3 : optimisticRetryCount;
    }
}
