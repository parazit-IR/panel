package com.parazit.panel.api.internal.referral;

import com.parazit.panel.application.referral.command.AssignReferralCommand;
import com.parazit.panel.application.referral.query.GetReferralOverviewQuery;
import com.parazit.panel.application.referral.result.AssignReferralResult;
import com.parazit.panel.application.referral.result.ReferralOverviewResult;
import org.springframework.stereotype.Component;

@Component
public class ReferralApiMapper {

    public GetReferralOverviewQuery toOverviewQuery(Long telegramUserId) {
        return new GetReferralOverviewQuery(telegramUserId);
    }

    public AssignReferralCommand toAssignCommand(Long telegramUserId, AssignReferralRequest request) {
        return new AssignReferralCommand(telegramUserId, request.referralCode());
    }

    public ReferralOverviewResponse toResponse(ReferralOverviewResult result) {
        return new ReferralOverviewResponse(
                result.userId(),
                result.telegramUserId(),
                result.referralCode(),
                result.referralCount(),
                result.referrerUserId(),
                result.referrerTelegramUserId()
        );
    }

    public AssignReferralResponse toResponse(AssignReferralResult result) {
        return new AssignReferralResponse(
                result.referralId(),
                result.referrerUserId(),
                result.referredUserId(),
                result.referralCodeUsed(),
                result.status(),
                result.referredAt(),
                result.newlyAssigned()
        );
    }
}
