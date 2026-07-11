package com.parazit.panel.api.internal.referral;

import com.parazit.panel.domain.referral.ReferralStatus;
import java.time.Instant;
import java.util.UUID;

public record AssignReferralResponse(
        UUID referralId,
        UUID referrerUserId,
        UUID referredUserId,
        String referralCodeUsed,
        ReferralStatus status,
        Instant referredAt,
        boolean newlyAssigned
) {
}
