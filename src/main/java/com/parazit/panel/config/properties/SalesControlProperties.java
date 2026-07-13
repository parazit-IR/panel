package com.parazit.panel.config.properties;

import java.time.Instant;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sales")
public record SalesControlProperties(
        boolean newPurchaseEnabled,
        boolean renewalEnabled,
        boolean trialEnabled,
        boolean manualPaymentEnabled,
        boolean onlinePaymentEnabled,
        boolean walletPaymentEnabled,
        boolean discountCodeEnabled,
        boolean giftCodeEnabled,
        String purchaseDisabledMessage,
        String manualPaymentDisabledMessage,
        String onlinePaymentDisabledMessage,
        Instant salesResumeAt
) {

    public SalesControlProperties {
        purchaseDisabledMessage = bounded(purchaseDisabledMessage);
        manualPaymentDisabledMessage = bounded(manualPaymentDisabledMessage);
        onlinePaymentDisabledMessage = bounded(onlinePaymentDisabledMessage);
    }

    @Override
    public String toString() {
        return "SalesControlProperties["
                + "newPurchaseEnabled=" + newPurchaseEnabled
                + ", renewalEnabled=" + renewalEnabled
                + ", trialEnabled=" + trialEnabled
                + ", manualPaymentEnabled=" + manualPaymentEnabled
                + ", onlinePaymentEnabled=" + onlinePaymentEnabled
                + ", walletPaymentEnabled=" + walletPaymentEnabled
                + ", discountCodeEnabled=" + discountCodeEnabled
                + ", giftCodeEnabled=" + giftCodeEnabled
                + ", salesResumeAt=" + salesResumeAt
                + ']';
    }

    private static String bounded(String value) {
        String normalized = Objects.requireNonNullElse(value, "").replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() > 500) {
            return normalized.substring(0, 500);
        }
        return normalized;
    }
}
