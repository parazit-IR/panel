package com.parazit.panel.domain.plan.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanSelectionTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void createsTrafficLimitedSnapshot() {
        Plan plan = activePlan(PlanTestData.trafficLimitedPlan("MONTHLY_30GB", 1));

        PlanSelection selection = PlanSelection.create(USER_ID, plan, NOW, TTL);

        assertThat(selection.getUserId()).isEqualTo(USER_ID);
        assertThat(selection.getPlanId()).isEqualTo(plan.getId());
        assertThat(selection.getPlanCodeSnapshot()).isEqualTo("MONTHLY_30GB");
        assertThat(selection.getPlanNameSnapshot()).isEqualTo("Monthly 30 GiB");
        assertThat(selection.getPlanTypeSnapshot()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(selection.getPriceAmountSnapshot()).isEqualTo(500_000L);
        assertThat(selection.getCurrencySnapshot()).isEqualTo(CurrencyCode.IRT);
        assertThat(selection.getDurationDaysSnapshot()).isEqualTo(30);
        assertThat(selection.getTrafficLimitBytesSnapshot()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(selection.getMaxDevicesSnapshot()).isEqualTo(2);
        assertThat(selection.getStatus()).isEqualTo(PlanSelectionStatus.ACTIVE);
        assertThat(selection.getSelectedAt()).isEqualTo(NOW);
        assertThat(selection.getExpiresAt()).isEqualTo(NOW.plus(TTL));
    }

    @Test
    void createsUnlimitedSnapshot() {
        Plan plan = activePlan(PlanTestData.unlimitedPlan("MONTHLY_UNLIMITED", 1));

        PlanSelection selection = PlanSelection.create(USER_ID, plan, NOW, TTL);

        assertThat(selection.getPlanTypeSnapshot()).isEqualTo(PlanType.UNLIMITED);
        assertThat(selection.getTrafficLimitBytesSnapshot()).isNull();
        assertThat(selection.getMaxDevicesSnapshot()).isNull();
    }

    @Test
    void rejectsInvalidCreationInputs() {
        Plan active = activePlan(PlanTestData.unlimitedPlan("ACTIVE_PLAN", 1));
        Plan draft = savedPlan(PlanTestData.unlimitedPlan("DRAFT_PLAN", 2));

        assertThatNullPointerException().isThrownBy(() -> PlanSelection.create(null, active, NOW, TTL))
                .withMessage("userId must not be null");
        assertThatNullPointerException().isThrownBy(() -> PlanSelection.create(USER_ID, null, NOW, TTL))
                .withMessage("plan must not be null");
        assertThatThrownBy(() -> PlanSelection.create(USER_ID, draft, NOW, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plan must be ACTIVE");
        assertThatNullPointerException().isThrownBy(() -> PlanSelection.create(USER_ID, active, null, TTL))
                .withMessage("selectedAt must not be null");
        assertThatThrownBy(() -> PlanSelection.create(USER_ID, active, NOW, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ttl must be positive");
        assertThatThrownBy(() -> PlanSelection.create(USER_ID, active, NOW, Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ttl must be positive");
    }

    @Test
    void detectsExpirationAtBoundaryOnlyForActiveSelection() {
        PlanSelection selection = PlanSelection.create(USER_ID, activePlan(PlanTestData.unlimitedPlan()), NOW, TTL);

        assertThat(selection.isExpiredAt(NOW.plus(TTL).minusNanos(1))).isFalse();
        assertThat(selection.isExpiredAt(NOW.plus(TTL))).isTrue();

        selection.clear(NOW.plusSeconds(1));

        assertThat(selection.isExpiredAt(NOW.plus(TTL).plusSeconds(1))).isFalse();
    }

    @Test
    void transitionsToExpiredClearedAndConsumed() {
        Plan plan = activePlan(PlanTestData.unlimitedPlan());
        PlanSelection expired = PlanSelection.create(USER_ID, plan, NOW, TTL);
        expired.expire(NOW.plus(TTL));
        assertThat(expired.getStatus()).isEqualTo(PlanSelectionStatus.EXPIRED);

        PlanSelection cleared = PlanSelection.create(USER_ID, plan, NOW, TTL);
        cleared.clear(NOW.plusSeconds(1));
        cleared.clear(NOW.plusSeconds(2));
        assertThat(cleared.getStatus()).isEqualTo(PlanSelectionStatus.CLEARED);

        PlanSelection consumed = PlanSelection.create(USER_ID, plan, NOW, TTL);
        consumed.consume(NOW.plusSeconds(1));
        assertThat(consumed.getStatus()).isEqualTo(PlanSelectionStatus.CONSUMED);
    }

    @Test
    void rejectsInvalidTerminalTransitions() {
        PlanSelection expired = PlanSelection.create(USER_ID, activePlan(PlanTestData.unlimitedPlan()), NOW, TTL);
        expired.expire(NOW.plus(TTL));

        assertThatThrownBy(() -> expired.clear(NOW.plus(TTL).plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot clear plan selection with status EXPIRED");
        assertThatThrownBy(() -> expired.consume(NOW.plus(TTL).plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot consume plan selection with status EXPIRED");
        assertThatThrownBy(() -> expired.expire(NOW.plus(TTL).plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot expire plan selection with status EXPIRED");
    }

    @Test
    void snapshotDoesNotChangeWhenPlanChangesLater() {
        Plan plan = activePlan(PlanTestData.trafficLimitedPlan("SNAPSHOT_PLAN", 1));
        PlanSelection selection = PlanSelection.create(USER_ID, plan, NOW, TTL);

        plan.updateDetails("Updated", null, PlanType.UNLIMITED, 10L, CurrencyCode.IRT, 60, null, null, 5);

        assertThat(selection.getPlanNameSnapshot()).isEqualTo("Monthly 30 GiB");
        assertThat(selection.getPlanTypeSnapshot()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(selection.getPriceAmountSnapshot()).isEqualTo(500_000L);
        assertThat(selection.getDurationDaysSnapshot()).isEqualTo(30);
        assertThat(selection.getTrafficLimitBytesSnapshot()).isEqualTo(PlanTestData.THIRTY_GIB);
    }

    private Plan activePlan(Plan plan) {
        Plan saved = savedPlan(plan);
        saved.activate();
        return savedPlan(saved);
    }

    private Plan savedPlan(Plan plan) {
        return new FakePlanRepository().save(plan);
    }
}
