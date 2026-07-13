package com.parazit.panel.domain.subscription;

import java.util.Objects;
import java.util.regex.Pattern;

public final class SubscriptionAccessToken {

    public static final String PREFIX = "sub_";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^sub_[A-Za-z0-9_-]{32,256}$");

    private final String rawToken;

    private SubscriptionAccessToken(String rawToken) {
        this.rawToken = normalize(rawToken);
    }

    public static SubscriptionAccessToken parse(String rawToken) {
        return new SubscriptionAccessToken(rawToken);
    }

    public String rawToken() {
        return rawToken;
    }

    public String safePrefix(int length) {
        if (length < PREFIX.length() || length > 32) {
            throw new IllegalArgumentException("prefix length is invalid");
        }
        return rawToken.substring(0, Math.min(length, rawToken.length()));
    }

    public static String normalize(String rawToken) {
        String normalized = Objects.requireNonNull(rawToken, "rawToken must not be null").trim();
        if (!TOKEN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Subscription token format is invalid");
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "SubscriptionAccessToken[redacted]";
    }
}
