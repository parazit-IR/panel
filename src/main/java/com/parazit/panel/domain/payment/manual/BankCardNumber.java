package com.parazit.panel.domain.payment.manual;

import java.util.Objects;

public final class BankCardNumber {

    private static final int CARD_NUMBER_LENGTH = 16;

    private final String normalized;

    private BankCardNumber(String normalized) {
        this.normalized = normalized;
    }

    public static BankCardNumber parse(String value) {
        Objects.requireNonNull(value, "card number must not be null");
        String normalized = normalizeDigits(value);
        if (normalized.length() != CARD_NUMBER_LENGTH) {
            throw new InvalidBankCardNumberException("card number must contain exactly 16 digits");
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (!Character.isDigit(normalized.charAt(index))) {
                throw new InvalidBankCardNumberException("card number contains invalid characters");
            }
        }
        if (!hasValidLuhnChecksum(normalized)) {
            throw new InvalidBankCardNumberException("card number checksum is invalid");
        }
        return new BankCardNumber(normalized);
    }

    public String value() {
        return normalized;
    }

    public String formatted() {
        return grouped(normalized);
    }

    public String masked() {
        return normalized.substring(0, 4) + "-****-****-" + normalized.substring(12);
    }

    @Override
    public String toString() {
        return masked();
    }

    private static String normalizeDigits(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == ' ' || ch == '-') {
                continue;
            }
            if (ch >= '\u06F0' && ch <= '\u06F9') {
                result.append((char) ('0' + ch - '\u06F0'));
                continue;
            }
            if (ch >= '\u0660' && ch <= '\u0669') {
                result.append((char) ('0' + ch - '\u0660'));
                continue;
            }
            result.append(ch);
        }
        return result.toString();
    }

    private static boolean hasValidLuhnChecksum(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = number.length() - 1; index >= 0; index--) {
            int digit = number.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private static String grouped(String value) {
        return value.substring(0, 4)
                + "-"
                + value.substring(4, 8)
                + "-"
                + value.substring(8, 12)
                + "-"
                + value.substring(12);
    }
}
