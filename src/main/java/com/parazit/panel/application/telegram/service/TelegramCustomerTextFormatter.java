package com.parazit.panel.application.telegram.service;

import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TelegramCustomerTextFormatter {

    public String traffic(long bytes, String language) {
        boolean fa = isFa(language);
        if (bytes < 1024L) {
            return number(bytes, fa) + (fa ? " بایت" : " B");
        }
        double value = bytes;
        String unit = fa ? " بایت" : " B";
        if (bytes >= 1024L * 1024L * 1024L) {
            value = bytes / (1024d * 1024d * 1024d);
            unit = fa ? " گیگابایت" : " GB";
        } else if (bytes >= 1024L * 1024L) {
            value = bytes / (1024d * 1024d);
            unit = fa ? " مگابایت" : " MB";
        } else if (bytes >= 1024L) {
            value = bytes / 1024d;
            unit = fa ? " کیلوبایت" : " KB";
        }
        String formatted = value >= 10 ? String.format(Locale.ROOT, "%.0f", value) : String.format(Locale.ROOT, "%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return digits(formatted, fa) + unit;
    }

    public String duration(Duration duration, String language) {
        boolean fa = isFa(language);
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return fa ? "کمتر از یک روز" : "less than a day";
        }
        long days = duration.toDays();
        if (days > 0) {
            return number(days, fa) + (fa ? " روز" : " days");
        }
        long hours = Math.max(1, duration.toHours());
        return number(hours, fa) + (fa ? " ساعت" : " hours");
    }

    public String number(long value, String language) {
        return number(value, isFa(language));
    }

    private static String number(long value, boolean fa) {
        return digits(Long.toString(value), fa);
    }

    private static String digits(String value, boolean fa) {
        if (!fa) {
            return value;
        }
        return value
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹')
                .replace('.', '٫');
    }

    private static boolean isFa(String language) {
        return language != null && language.toUpperCase(Locale.ROOT).startsWith("FA");
    }
}
