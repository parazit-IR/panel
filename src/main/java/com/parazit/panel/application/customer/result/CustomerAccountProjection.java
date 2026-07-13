package com.parazit.panel.application.customer.result;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record CustomerAccountProjection(
        UUID userId,
        long telegramUserId,
        String displayName,
        String username,
        Instant registeredAt,
        String locale,
        boolean telegramNotificationsEnabled,
        Optional<String> referralCode,
        Optional<String> phoneNumberMasked,
        Optional<String> customerGroup
) {

    public CustomerAccountProjection {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        displayName = displayName == null || displayName.isBlank() ? "Customer" : displayName.trim();
        username = username == null || username.isBlank() ? "" : username.trim();
        locale = locale == null || locale.isBlank() ? "EN" : locale.trim();
        referralCode = referralCode == null ? Optional.empty() : referralCode;
        phoneNumberMasked = phoneNumberMasked == null ? Optional.empty() : phoneNumberMasked;
        customerGroup = customerGroup == null ? Optional.empty() : customerGroup;
    }
}
