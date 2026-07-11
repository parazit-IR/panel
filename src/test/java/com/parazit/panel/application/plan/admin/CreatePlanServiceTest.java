package com.parazit.panel.application.plan.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import org.junit.jupiter.api.Test;

class CreatePlanServiceTest {

    @Test
    void createsTrafficLimitedPlanWithDraftStatusAndNormalizedCode() {
        FakePlanRepository repository = new FakePlanRepository();
        CreatePlanService service = new CreatePlanService(repository, new PlanResultMapper());

        PlanResult result = service.create(new CreatePlanCommand(
                " monthly_30gb ",
                "Monthly 30 GiB",
                "30 GiB plan",
                PlanType.TRAFFIC_LIMITED,
                500_000L,
                CurrencyCode.IRT,
                30,
                PlanTestData.THIRTY_GIB,
                2,
                1
        ));

        assertThat(result.id()).isNotNull();
        assertThat(result.code()).isEqualTo("MONTHLY_30GB");
        assertThat(result.status()).isEqualTo(PlanStatus.DRAFT);
        assertThat(result.available()).isFalse();
        assertThat(result.trafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(repository.existsByCodeCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void createsUnlimitedPlan() {
        FakePlanRepository repository = new FakePlanRepository();
        CreatePlanService service = new CreatePlanService(repository, new PlanResultMapper());

        PlanResult result = service.create(new CreatePlanCommand(
                "MONTHLY_UNLIMITED",
                "Monthly Unlimited",
                null,
                PlanType.UNLIMITED,
                900_000L,
                CurrencyCode.IRT,
                30,
                null,
                null,
                2
        ));

        assertThat(result.code()).isEqualTo("MONTHLY_UNLIMITED");
        assertThat(result.type()).isEqualTo(PlanType.UNLIMITED);
        assertThat(result.trafficLimitBytes()).isNull();
        assertThat(result.maxDevices()).isNull();
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateCodeBeforeSave() {
        FakePlanRepository repository = new FakePlanRepository();
        repository.save(PlanTestData.unlimitedPlan("MONTHLY_UNLIMITED", 1));
        repository.saveCalls = 0;
        CreatePlanService service = new CreatePlanService(repository, new PlanResultMapper());

        assertThatThrownBy(() -> service.create(new CreatePlanCommand(
                " monthly_unlimited ",
                "Duplicate",
                null,
                PlanType.UNLIMITED,
                0,
                CurrencyCode.IRT,
                30,
                null,
                null,
                1
        )))
                .isInstanceOf(PlanCodeAlreadyExistsException.class)
                .hasMessage("Plan code already exists: MONTHLY_UNLIMITED");
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void translatesDuplicateKeyRaceToPlanCodeConflict() {
        FakePlanRepository repository = new FakePlanRepository();
        repository.failNextSaveWithDuplicateKey();
        CreatePlanService service = new CreatePlanService(repository, new PlanResultMapper());

        assertThatThrownBy(() -> service.create(new CreatePlanCommand(
                "RACE_PLAN",
                "Race Plan",
                null,
                PlanType.UNLIMITED,
                0,
                CurrencyCode.IRT,
                30,
                null,
                null,
                1
        )))
                .isInstanceOf(PlanCodeAlreadyExistsException.class)
                .hasMessage("Plan code already exists: RACE_PLAN");
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void rejectsInvalidCommandAndInvalidTrafficCombination() {
        CreatePlanService service = new CreatePlanService(new FakePlanRepository(), new PlanResultMapper());

        assertThatNullPointerException()
                .isThrownBy(() -> service.create(null))
                .withMessage("command must not be null");
        assertThatThrownBy(() -> service.create(new CreatePlanCommand(
                "UNLIMITED",
                "Unlimited",
                null,
                PlanType.UNLIMITED,
                0,
                CurrencyCode.IRT,
                30,
                PlanTestData.THIRTY_GIB,
                null,
                1
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("trafficLimitBytes must be null for unlimited plans");
    }
}
