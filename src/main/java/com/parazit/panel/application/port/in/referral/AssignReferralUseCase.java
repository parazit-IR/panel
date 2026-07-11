package com.parazit.panel.application.port.in.referral;

import com.parazit.panel.application.referral.command.AssignReferralCommand;
import com.parazit.panel.application.referral.result.AssignReferralResult;

public interface AssignReferralUseCase {

    AssignReferralResult assign(AssignReferralCommand command);
}
