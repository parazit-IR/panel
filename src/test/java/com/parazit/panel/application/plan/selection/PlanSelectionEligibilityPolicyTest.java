package com.parazit.panel.application.plan.selection;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlanSelectionEligibilityPolicyTest {

    private final PlanSelectionEligibilityPolicy policy = new PlanSelectionEligibilityPolicy();

    @Test
    void allowsActiveUnblockedUser() {
        User user = user();

        assertThatCode(() -> policy.verifyEligible(user)).doesNotThrowAnyException();
    }

    @Test
    void rejectsBlockedSuspendedAndInactiveUsersWithoutMutation() {
        User blocked = user();
        blocked.block();
        User suspended = user();
        suspended.suspend();
        User inactive = user();
        inactive.deactivate();

        assertThatThrownBy(() -> policy.verifyEligible(blocked))
                .isInstanceOf(UserNotEligibleForPlanSelectionException.class);
        assertThatThrownBy(() -> policy.verifyEligible(suspended))
                .isInstanceOf(UserNotEligibleForPlanSelectionException.class);
        assertThatThrownBy(() -> policy.verifyEligible(inactive))
                .isInstanceOf(UserNotEligibleForPlanSelectionException.class);
    }

    private User user() {
        return User.create(1001L, "user", "Ali", null, UserLanguage.FA, Instant.parse("2026-07-11T00:00:00Z"));
    }
}
