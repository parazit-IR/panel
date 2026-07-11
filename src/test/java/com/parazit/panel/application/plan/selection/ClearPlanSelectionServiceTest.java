package com.parazit.panel.application.plan.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
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

class ClearPlanSelectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void clearsActiveSelectionWithoutDeletingRow() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(3001L);
        Plan plan = fixture.activePlan(PlanTestData.unlimitedPlan("CLEAR_PLAN", 1));
        PlanSelection selection = fixture.selections.save(PlanSelection.create(user.getId(), plan, NOW, TTL));

        PlanSelectionResult result = fixture.service().clear(new ClearPlanSelectionCommand(3001L));

        assertThat(result.selectionId()).isEqualTo(selection.getId());
        assertThat(result.status()).isEqualTo(PlanSelectionStatus.CLEARED);
        assertThat(fixture.selections.count()).isEqualTo(1);
        assertThat(fixture.selections.deleteCalls).isZero();
    }

    @Test
    void expiredSelectionIsExpiredAndReportedAsNotFound() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(3002L);
        Plan plan = fixture.activePlan(PlanTestData.unlimitedPlan("EXPIRED_CLEAR", 1));
        PlanSelection selection = fixture.selections.save(PlanSelection.create(user.getId(), plan, NOW.minus(TTL), TTL));

        assertThatThrownBy(() -> fixture.service().clear(new ClearPlanSelectionCommand(3002L)))
                .isInstanceOf(PlanSelectionNotFoundException.class);
        assertThat(selection.getStatus()).isEqualTo(PlanSelectionStatus.EXPIRED);
    }

    @Test
    void rejectsMissingUserOrSelection() {
        Fixture fixture = new Fixture();
        fixture.activeUser(3003L);

        assertThatThrownBy(() -> fixture.service().clear(new ClearPlanSelectionCommand(404L)))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> fixture.service().clear(new ClearPlanSelectionCommand(3003L)))
                .isInstanceOf(PlanSelectionNotFoundException.class);
    }

    private static final class Fixture {
        private final FakeUserRepository users = new FakeUserRepository();
        private final FakePlanRepository plans = new FakePlanRepository();
        private final FakePlanSelectionRepository selections = new FakePlanSelectionRepository();

        ClearPlanSelectionService service() {
            return new ClearPlanSelectionService(users, selections, () -> NOW, new PlanSelectionResultMapper());
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
