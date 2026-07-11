package com.parazit.panel.application.referral.result;

import java.util.UUID;

public record ReferralOverviewResult(
        UUID userId,
        Long telegramUserId,
        String referralCode,
        long referralCount,
        UUID referrerUserId,
        Long referrerTelegramUserId
) {
}
