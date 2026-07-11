package com.parazit.panel.application.referral.query;

import java.util.Objects;

public record GetReferralOverviewQuery(Long telegramUserId) {

    public GetReferralOverviewQuery {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }
}
