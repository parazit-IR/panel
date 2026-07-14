package com.parazit.panel.application.promotion;

import com.parazit.panel.config.properties.PromotionProperties;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PromotionCodeNormalizer {

    private final PromotionProperties properties;

    public PromotionCodeNormalizer(PromotionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public String normalize(String rawCode) {
        String trimmed = Objects.requireNonNull(rawCode, "rawCode must not be null").trim();
        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isISOControl(ch) || Character.isWhitespace(ch)) {
                throw new PromotionException("telegram.promotion.invalid_code");
            }
            builder.append(normalizeCharacter(ch));
        }
        String normalized = builder.toString().toUpperCase(Locale.ROOT);
        if (normalized.length() < properties.codeMinLength() || normalized.length() > properties.codeMaxLength()) {
            throw new PromotionException("telegram.promotion.invalid_code");
        }
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            boolean allowed = (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-';
            if (!allowed) {
                throw new PromotionException("telegram.promotion.invalid_code");
            }
        }
        return normalized;
    }

    public String mask(String normalizedCode) {
        String code = normalize(normalizedCode);
        if (code.length() <= 4) {
            return "****";
        }
        return code.substring(0, Math.min(2, code.length())) + "***" + code.substring(code.length() - 2);
    }

    private static char normalizeCharacter(char ch) {
        if (ch >= '۰' && ch <= '۹') {
            return (char) ('0' + (ch - '۰'));
        }
        if (ch >= '٠' && ch <= '٩') {
            return (char) ('0' + (ch - '٠'));
        }
        if (ch == 'ي') {
            return 'ی';
        }
        if (ch == 'ك') {
            return 'ک';
        }
        return ch;
    }
}
