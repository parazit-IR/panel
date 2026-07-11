package com.parazit.panel.application.plan.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.config.properties.PlanSelectionProperties;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
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

class SelectPlanServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void createsFirstSelectionWithSnapshotAndFixedClock() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(1001L);
        Plan plan = fixture.activePlan(PlanTestData.trafficLimitedPlan("LIMITED_PLAN", 1));

        PlanSelectionResult result = fixture.service().select(new SelectPlanCommand(1001L, plan.getId()));

        assertThat(result.selectionId()).isNotNull();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.telegramUserId()).isEqualTo(1001L);
        assertThat(result.planId()).isEqualTo(plan.getId());
        assertThat(result.planCode()).isEqualTo("LIMITED_PLAN");
        assertThat(result.planName()).isEqualTo("Monthly 30 GiB");
        assertThat(result.planType()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(result.priceAmount()).isEqualTo(500_000L);
        assertThat(result.currency()).isEqualTo(CurrencyCode.IRT);
        assertThat(result.durationDays()).isEqualTo(30);
        assertThat(result.trafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(result.maxDevices()).isEqualTo(2);
        assertThat(result.status()).isEqualTo(PlanSelectionStatus.ACTIVE);
        assertThat(result.selectedAt()).isEqualTo(NOW);
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(result.newlySelected()).isTrue();
        assertThat(fixture.selections.saveCalls).isEqualTo(1);
    }

    @Test
    void selectingSameCurrentPlanIsIdempotent() {
        Fixture fixture = new Fixture();
        fixture.activeUser(1002L);
        Plan plan = fixture.activePlan(PlanTestData.unlimitedPlan("SAME_PLAN", 1));
        PlanSelectionResult first = fixture.service().select(new SelectPlanCommand(1002L, plan.getId()));

        PlanSelectionResult second = fixture.service().select(new SelectPlanCommand(1002L, plan.getId()));

        assertThat(second.selectionId()).isEqualTo(first.selectionId());
        assertThat(second.newlySelected()).isFalse();
        assertThat(fixture.selections.count()).isEqualTo(1);
    }

    @Test
    void selectingDifferentPlanClearsOldSelectionAndCreatesNewOne() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(1003L);
        Plan firstPlan = fixture.activePlan(PlanTestData.unlimitedPlan("FIRST_PLAN", 1));
        Plan secondPlan = fixture.activePlan(PlanTestData.trafficLimitedPlan("SECOND_PLAN", 2));
        PlanSelectionResult first = fixture.service().select(new SelectPlanCommand(1003L, firstPlan.getId()));

        PlanSelectionResult second = fixture.service().select(new SelectPlanCommand(1003L, secondPlan.getId()));

        assertThat(second.selectionId()).isNotEqualTo(first.selectionId());
        assertThat(second.planCode()).isEqualTo("SECOND_PLAN");
        assertThat(second.newlySelected()).isTrue();
        assertThat(fixture.selections.findById(first.selectionId()).orElseThrow().getStatus())
                .isEqualTo(PlanSelectionStatus.CLEARED);
        assertThat(fixture.selections.findActiveByUserId(user.getId()).orElseThrow().getId())
                .isEqualTo(second.selectionId());
    }

    @Test
    void expiredCurrentSelectionIsExpiredThenReplaced() {
        Fixture fixture = new Fixture();
        User user = fixture.activeUser(1004L);
        Plan firstPlan = fixture.activePlan(PlanTestData.unlimitedPlan("EXPIRED_OLD", 1));
        Plan secondPlan = fixture.activePlan(PlanTestData.unlimitedPlan("EXPIRED_NEW", 2));
        PlanSelection old = PlanSelection.create(user.getId(), firstPlan, NOW.minus(TTL), TTL);
        fixture.selections.save(old);

        PlanSelectionResult result = fixture.service().select(new SelectPlanCommand(1004L, secondPlan.getId()));

        assertThat(old.getStatus()).isEqualTo(PlanSelectionStatus.EXPIRED);
        assertThat(result.planCode()).isEqualTo("EXPIRED_NEW");
        assertThat(result.newlySelected()).isTrue();
        assertThat(fixture.selections.findActiveByUserId(user.getId()).orElseThrow().getId())
                .isEqualTo(result.selectionId());
    }

    @Test
    void rejectsMissingOrIneligibleInputs() {
        Fixture fixture = new Fixture();
        User blocked = fixture.activeUser(1005L);
        blocked.block();
        Plan draft = fixture.savedPlan(PlanTestData.unlimitedPlan("DRAFT_PLAN", 1));

        assertThatNullPointerException().isThrownBy(() -> fixture.service().select(null))
                .withMessage("command must not be null");
        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(null, draft.getId())))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("telegramUserId must not be null");
        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(-1L, draft.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telegramUserId must be positive");
        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(1005L, null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("planId must not be null");
        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(404L, draft.getId())))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(1005L, draft.getId())))
                .isInstanceOf(UserNotEligibleForPlanSelectionException.class);

        blocked.unblock();
        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(1005L, draft.getId())))
                .isInstanceOf(AvailablePlanNotFoundException.class);
    }

    @Test
    void translatesActiveSelectionUniquenessConflict() {
        Fixture fixture = new Fixture();
        fixture.activeUser(1006L);
        Plan plan = fixture.activePlan(PlanTestData.unlimitedPlan("CONFLICT_PLAN", 1));
        fixture.selections.failNextSaveWithDataIntegrityViolation();

        assertThatThrownBy(() -> fixture.service().select(new SelectPlanCommand(1006L, plan.getId())))
                .isInstanceOf(PlanSelectionConflictException.class);
    }

    private static final class Fixture {
        private final FakeUserRepository users = new FakeUserRepository();
        private final FakePlanRepository plans = new FakePlanRepository();
        private final FakePlanSelectionRepository selections = new FakePlanSelectionRepository();

        SelectPlanService service() {
            return new SelectPlanService(
                    users,
                    plans,
                    selections,
                    () -> NOW,
                    new PlanSelectionProperties(TTL),
                    new PlanSelectionEligibilityPolicy(),
                    new PlanSelectionResultMapper()
            );
        }

        User activeUser(Long telegramUserId) {
            return users.save(User.create(telegramUserId, "user" + telegramUserId, "Ali", null, UserLanguage.FA, NOW));
        }

        Plan activePlan(Plan plan) {
            Plan saved = savedPlan(plan);
            saved.activate();
            return plans.save(saved);
        }

        Plan savedPlan(Plan plan) {
            return plans.save(plan);
        }
    }
}
