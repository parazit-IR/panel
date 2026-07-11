package com.parazit.panel.api.internal.referral;

import java.util.UUID;

public record ReferralOverviewResponse(
        UUID userId,
        Long telegramUserId,
        String referralCode,
        long referralCount,
        UUID referrerUserId,
        Long referrerTelegramUserId
) {
}
