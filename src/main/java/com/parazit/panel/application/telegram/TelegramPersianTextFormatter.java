package com.parazit.panel.application.telegram;

import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TelegramPersianTextFormatter {

    private static final Locale PERSIAN = Locale.forLanguageTag("fa-IR");

    public String formatNumber(long value, String language) {
        if (language != null && language.toUpperCase(Locale.ROOT).startsWith("FA")) {
            return NumberFormat.getIntegerInstance(PERSIAN).format(value);
        }
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    public String formatAmount(long amount, String currency, String language) {
        String formatted = formatNumber(amount, language);
        if (currency == null || currency.isBlank()) {
            return formatted;
        }
        return formatted + " " + currency;
    }
}
