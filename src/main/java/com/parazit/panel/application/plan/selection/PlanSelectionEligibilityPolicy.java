package com.parazit.panel.application.plan.selection;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PlanSelectionEligibilityPolicy {

    public void verifyEligible(User user) {
        Objects.requireNonNull(user, "user must not be null");
        if (Boolean.TRUE.equals(user.getBlocked()) || user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotEligibleForPlanSelectionException(user.getTelegramUserId());
        }
    }
}
