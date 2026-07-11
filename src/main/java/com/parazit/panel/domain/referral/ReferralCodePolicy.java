package com.parazit.panel.domain.referral;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ReferralCodePolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 16;
    public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final String REGEX = "^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8,16}$";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private ReferralCodePolicy() {
    }

    public static String normalizeAndValidate(String referralCode) {
        Objects.requireNonNull(referralCode, "referralCode must not be null");
        String normalized = referralCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("referralCode must not be blank");
        }
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("referralCode must be 8 to 16 non-ambiguous uppercase alphanumeric characters");
        }
        return normalized;
    }
}
