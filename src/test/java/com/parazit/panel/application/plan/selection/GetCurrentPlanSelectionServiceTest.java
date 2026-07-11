package com.parazit.panel.application.plan.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.FakePlanSelectionRepository;
import com.parazit.panel.test.fixture.FakeUserRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetCurrentPlanSelectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void returnsValidCurrentSelectionWithoutLoadingPlan() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(2001L);
        Plan plan = fixture.activePlan(PlanTestData.unlimitedPlan("CURRENT_PLAN", 1));
        PlanSelection selection = fixture.selections.save(PlanSelection.create(user.getId(), plan, NOW, TTL));

        PlanSelectionResult result = fixture.service().getCurrent(new GetCurrentPlanSelectionQuery(2001L));

        assertThat(result.selectionId()).isEqualTo(selection.getId());
        assertThat(result.planCode()).isEqualTo("CURRENT_PLAN");
        assertThat(result.newlySelected()).isFalse();
        assertThat(fixture.plans.findByIdCalls).isZero();
    }

    @Test
    void marksExpiredSelectionAndReturnsNotFound() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(2002L);
        Plan plan = fixture.activePlan(PlanTestData.unlimitedPlan("EXPIRED_CURRENT", 1));
        PlanSelection selection = fixture.selections.save(PlanSelection.create(user.getId(), plan, NOW.minus(TTL), TTL));

        assertThatThrownBy(() -> fixture.service().getCurrent(new GetCurrentPlanSelectionQuery(2002L)))
                .isInstanceOf(PlanSelectionNotFoundException.class);
        assertThat(selection.getStatus()).isEqualTo(PlanSelectionStatus.EXPIRED);
    }

    @Test
    void rejectsMissingUserOrSelection() {
        Fixture fixture = new Fixture();
        fixture.activeUser(2003L);

        assertThatThrownBy(() -> fixture.service().getCurrent(new GetCurrentPlanSelectionQuery(404L)))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> fixture.service().getCurrent(new GetCurrentPlanSelectionQuery(2003L)))
                .isInstanceOf(PlanSelectionNotFoundException.class);
    }

    private static final class Fixture {
        private final FakeUserRepository users = new FakeUserRepository();
        private final FakePlanRepository plans = new FakePlanRepository();
        private final FakePlanSelectionRepository selections = new FakePlanSelectionRepository();

        GetCurrentPlanSelectionService service() {
            return new GetCurrentPlanSelectionService(users, selections, () -> NOW, new PlanSelectionResultMapper());
        }

        User activeUser(Long telegramUserId) {
            return users.save(User.create(telegramUserId, "user" + telegramUserId, "Ali", null, UserLanguage.FA, NOW));
        }

        Plan activePlan(Plan plan) {
            Plan saved = plans.save(plan);
            saved.activate();
            return plans.save(saved);
        }
    }
}
