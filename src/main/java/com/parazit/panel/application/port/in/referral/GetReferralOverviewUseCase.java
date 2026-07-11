package com.parazit.panel.application.port.in.referral;

import com.parazit.panel.application.referral.query.GetReferralOverviewQuery;
import com.parazit.panel.application.referral.result.ReferralOverviewResult;

public interface GetReferralOverviewUseCase {

    ReferralOverviewResult getOverview(GetReferralOverviewQuery query);
}
