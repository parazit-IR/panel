package com.parazit.panel.application.referral.result;

import com.parazit.panel.domain.referral.Referral;
import com.parazit.panel.domain.referral.ReferralStatus;
import java.time.Instant;
import java.util.UUID;

public record AssignReferralResult(
        UUID referralId,
        UUID referrerUserId,
        UUID referredUserId,
        String referralCodeUsed,
        ReferralStatus status,
        Instant referredAt,
        boolean newlyAssigned
) {

    public static AssignReferralResult from(Referral referral, boolean newlyAssigned) {
        return new AssignReferralResult(
                referral.getId(),
                referral.getReferrerUserId(),
                referral.getReferredUserId(),
                referral.getReferralCodeUsed(),
                referral.getStatus(),
                referral.getReferredAt(),
                newlyAssigned
        );
    }
}
