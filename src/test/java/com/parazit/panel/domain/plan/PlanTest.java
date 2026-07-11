package com.parazit.panel.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.parazit.panel.test.fixture.PlanTestData;
import org.junit.jupiter.api.Test;

class PlanTest {

    private static final long THIRTY_GIB = PlanTestData.THIRTY_GIB;

    @Test
    void createsTrafficLimitedPlanWithDraftStatusAndNormalizedValues() {
        Plan plan = Plan.create(
                " monthly_30gb ",
                " Monthly 30 GiB ",
                "  30 GiB traffic  ",
                PlanType.TRAFFIC_LIMITED,
                500_000L,
                CurrencyCode.IRT,
                30,
                THIRTY_GIB,
                2,
                1
        );

        assertThat(plan.getCode()).isEqualTo("MONTHLY_30GB");
        assertThat(plan.getName()).isEqualTo("Monthly 30 GiB");
        assertThat(plan.getDescription()).isEqualTo("30 GiB traffic");
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DRAFT);
        assertThat(plan.getType()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(plan.getPriceAmount()).isEqualTo(500_000L);
        assertThat(plan.getCurrency()).isEqualTo(CurrencyCode.IRT);
        assertThat(plan.getDurationDays()).isEqualTo(30);
        assertThat(plan.getTrafficLimitBytes()).isEqualTo(THIRTY_GIB);
        assertThat(plan.getMaxDevices()).isEqualTo(2);
        assertThat(plan.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void createsUnlimitedPlanWithNullTrafficAndBlankDescriptionAsNull() {
        Plan plan = Plan.create(
                "MONTHLY_UNLIMITED",
                "Unlimited",
                "   ",
                PlanType.UNLIMITED,
                900_000L,
                CurrencyCode.IRT,
                30,
                null,
                null,
                0
        );

        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DRAFT);
        assertThat(plan.getType()).isEqualTo(PlanType.UNLIMITED);
        assertThat(plan.getTrafficLimitBytes()).isNull();
        assertThat(plan.getDescription()).isNull();
        assertThat(plan.getMaxDevices()).isNull();
    }

    @Test
    void rejectsInvalidCode() {
        assertThatNullPointerException()
                .isThrownBy(() -> create(null, "Name", PlanType.UNLIMITED, null))
                .withMessage("code must not be null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("   ", "Name", PlanType.UNLIMITED, null))
                .withMessage("code must not be blank");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("AB", "Name", PlanType.UNLIMITED, null))
                .withMessage("code must be at least 3 characters");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("MONTHLY 30GB", "Name", PlanType.TRAFFIC_LIMITED, THIRTY_GIB))
                .withMessage("code may contain only uppercase letters, digits, underscores, and hyphens");
    }

    @Test
    void rejectsOverlyLongCodeNameAndDescription() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("A".repeat(65), "Name", PlanType.UNLIMITED, null))
                .withMessage("code must be at most 64 characters");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("MONTHLY", "A".repeat(129), PlanType.UNLIMITED, null))
                .withMessage("name must be at most 128 characters");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create(
                        "MONTHLY",
                        "Name",
                        "A".repeat(1001),
                        PlanType.UNLIMITED,
                        0,
                        CurrencyCode.IRT,
                        1,
                        null,
                        null,
                        0
                ))
                .withMessage("description must be at most 1000 characters");
    }

    @Test
    void rejectsBlankNameAndNullRequiredValues() {
        assertThatNullPointerException()
                .isThrownBy(() -> create("MONTHLY", null, PlanType.UNLIMITED, null))
                .withMessage("name must not be null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("MONTHLY", "   ", PlanType.UNLIMITED, null))
                .withMessage("name must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> create("MONTHLY", "Name", null, null))
                .withMessage("type must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, 0, null, 30, null, null, 0))
                .withMessage("currency must not be null");
    }

    @Test
    void rejectsInvalidAmountsDurationDisplayOrderAndMaxDevices() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, -1, CurrencyCode.IRT, 30, null, null, 0))
                .withMessage("priceAmount must be zero or positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, 0, CurrencyCode.IRT, 0, null, null, 0))
                .withMessage("durationDays must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, 0, CurrencyCode.IRT, -1, null, null, 0))
                .withMessage("durationDays must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, 0, CurrencyCode.IRT, 30, null, null, -1))
                .withMessage("displayOrder must be zero or positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, 0, CurrencyCode.IRT, 30, null, 0, 0))
                .withMessage("maxDevices must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Plan.create("MONTHLY", "Name", null, PlanType.UNLIMITED, 0, CurrencyCode.IRT, 30, null, -1, 0))
                .withMessage("maxDevices must be positive");
    }

    @Test
    void validatesTrafficRulesByPlanType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("LIMITED", "Name", PlanType.TRAFFIC_LIMITED, null))
                .withMessage("trafficLimitBytes is required for traffic-limited plans");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("LIMITED", "Name", PlanType.TRAFFIC_LIMITED, 0L))
                .withMessage("trafficLimitBytes must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("LIMITED", "Name", PlanType.TRAFFIC_LIMITED, -1L))
                .withMessage("trafficLimitBytes must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> create("UNLIMITED", "Name", PlanType.UNLIMITED, THIRTY_GIB))
                .withMessage("trafficLimitBytes must be null for unlimited plans");
    }

    @Test
    void updatesDetailsWithoutChangingCodeOrStatus() {
        Plan plan = PlanTestData.trafficLimitedPlan("MONTHLY_30GB", 1);

        plan.updateDetails(" Unlimited ", "   ", PlanType.UNLIMITED, 700_000L, CurrencyCode.IRT, 60, null, null, 5);

        assertThat(plan.getCode()).isEqualTo("MONTHLY_30GB");
        assertThat(plan.getStatus()).isEqualTo(PlanStatus.DRAFT);
        assertThat(plan.getName()).isEqualTo("Unlimited");
        assertThat(plan.getDescription()).isNull();
        assertThat(plan.getType()).isEqualTo(PlanType.UNLIMITED);
        assertThat(plan.getPriceAmount()).isEqualTo(700_000L);
        assertThat(plan.getDurationDays()).isEqualTo(60);
        assertThat(plan.getTrafficLimitBytes()).isNull();
        assertThat(plan.getMaxDevices()).isNull();
        assertThat(plan.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void updateDetailsCannotBreakTrafficInvariants() {
        Plan plan = PlanTestData.trafficLimitedPlan();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> plan.updateDetails("Unlimited", null, PlanType.UNLIMITED, 700_000L, CurrencyCode.IRT, 30, THIRTY_GIB, null, 1))
                .withMessage("trafficLimitBytes must be null for unlimited plans");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> plan.updateDetails("Limited", null, PlanType.TRAFFIC_LIMITED, 700_000L, CurrencyCode.IRT, 30, null, null, 1))
                .withMessage("trafficLimitBytes is required for traffic-limited plans");
    }

    @Test
    void supportsAllowedLifecycleTransitions() {
        Plan draftToActive = PlanTestData.trafficLimitedPlan("DRAFT_ACTIVE", 1);
        draftToActive.activate();
        assertThat(draftToActive.getStatus()).isEqualTo(PlanStatus.ACTIVE);

        draftToActive.deactivate();
        assertThat(draftToActive.getStatus()).isEqualTo(PlanStatus.INACTIVE);

        draftToActive.activate();
        assertThat(draftToActive.getStatus()).isEqualTo(PlanStatus.ACTIVE);

        Plan draftToArchived = PlanTestData.trafficLimitedPlan("DRAFT_ARCHIVED", 1);
        draftToArchived.archive();
        assertThat(draftToArchived.getStatus()).isEqualTo(PlanStatus.ARCHIVED);

        Plan activeToArchived = PlanTestData.trafficLimitedPlan("ACTIVE_ARCHIVED", 1);
        activeToArchived.activate();
        activeToArchived.archive();
        assertThat(activeToArchived.getStatus()).isEqualTo(PlanStatus.ARCHIVED);

        Plan inactiveToArchived = PlanTestData.trafficLimitedPlan("INACTIVE_ARCHIVED", 1);
        inactiveToArchived.activate();
        inactiveToArchived.deactivate();
        inactiveToArchived.archive();
        assertThat(inactiveToArchived.getStatus()).isEqualTo(PlanStatus.ARCHIVED);
    }

    @Test
    void rejectsInvalidAndSameStateLifecycleTransitionsDeterministically() {
        Plan draft = PlanTestData.trafficLimitedPlan("DRAFT", 1);
        assertThatIllegalStateException()
                .isThrownBy(draft::deactivate)
                .withMessage("cannot deactivate plan with status DRAFT");

        Plan active = PlanTestData.trafficLimitedPlan("ACTIVE", 1);
        active.activate();
        assertThatIllegalStateException()
                .isThrownBy(active::activate)
                .withMessage("cannot activate plan with status ACTIVE");

        Plan inactive = PlanTestData.trafficLimitedPlan("INACTIVE", 1);
        inactive.activate();
        inactive.deactivate();
        assertThatIllegalStateException()
                .isThrownBy(inactive::deactivate)
                .withMessage("cannot deactivate plan with status INACTIVE");
    }

    @Test
    void archivedPlanCannotChangeStateOrDetails() {
        Plan plan = PlanTestData.trafficLimitedPlan("ARCHIVED", 1);
        plan.archive();

        assertThatIllegalStateException()
                .isThrownBy(plan::activate)
                .withMessage("cannot activate plan with status ARCHIVED");
        assertThatIllegalStateException()
                .isThrownBy(plan::deactivate)
                .withMessage("cannot deactivate plan with status ARCHIVED");
        assertThatIllegalStateException()
                .isThrownBy(plan::archive)
                .withMessage("cannot archive plan with status ARCHIVED");
        assertThatIllegalStateException()
                .isThrownBy(() -> plan.updateDetails("Name", null, PlanType.UNLIMITED, 0, CurrencyCode.IRT, 30, null, null, 1))
                .withMessage("archived plans cannot be updated");
    }

    private Plan create(String code, String name, PlanType type, Long trafficLimitBytes) {
        return Plan.create(code, name, null, type, 0, CurrencyCode.IRT, 30, trafficLimitBytes, null, 0);
    }
}
