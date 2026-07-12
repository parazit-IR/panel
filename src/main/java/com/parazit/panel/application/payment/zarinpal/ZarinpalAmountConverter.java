package com.parazit.panel.application.payment.zarinpal;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ZarinpalAmountConverter {

    public long toGatewayAmount(long localAmount, String currency) {
        if (localAmount <= 0) {
            throw new IllegalArgumentException("localAmount must be positive");
        }
        String normalizedCurrency = normalizeCurrency(currency);
        if (!"IRT".equals(normalizedCurrency)) {
            throw new IllegalArgumentException("Unsupported Zarinpal currency: " + normalizedCurrency);
        }
        return localAmount;
    }

    public String gatewayCurrency(String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        if (!"IRT".equals(normalizedCurrency)) {
            throw new IllegalArgumentException("Unsupported Zarinpal currency: " + normalizedCurrency);
        }
        return normalizedCurrency;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency must not be null");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        return normalized;
    }
}
