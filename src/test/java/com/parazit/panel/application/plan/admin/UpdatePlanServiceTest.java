package com.parazit.panel.application.plan.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.command.UpdatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdatePlanServiceTest {

    @Test
    void updatesDetailsAndPreservesCodeAndStatus() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan plan = repository.save(PlanTestData.trafficLimitedPlan("MONTHLY_30GB", 1));
        repository.saveCalls = 0;

        PlanResult result = new UpdatePlanService(repository, new PlanResultMapper()).update(new UpdatePlanCommand(
                plan.getId(),
                "Updated Unlimited",
                "Updated details",
                PlanType.UNLIMITED,
                700_000L,
                CurrencyCode.IRT,
                60,
                null,
                null,
                4
        ));

        assertThat(result.id()).isEqualTo(plan.getId());
        assertThat(result.code()).isEqualTo("MONTHLY_30GB");
        assertThat(result.status()).isEqualTo(PlanStatus.DRAFT);
        assertThat(result.name()).isEqualTo("Updated Unlimited");
        assertThat(result.type()).isEqualTo(PlanType.UNLIMITED);
        assertThat(result.trafficLimitBytes()).isNull();
        assertThat(repository.findByIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void rejectsMissingArchivedInvalidAndNullUpdates() {
        FakePlanRepository repository = new FakePlanRepository();
        UpdatePlanService service = new UpdatePlanService(repository, new PlanResultMapper());
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThatThrownBy(() -> service.update(validCommand(missingId)))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found for id " + missingId);

        Plan archived = repository.save(PlanTestData.trafficLimitedPlan("ARCHIVED_PLAN", 1));
        archived.archive();
        assertThatThrownBy(() -> service.update(validCommand(archived.getId())))
                .isInstanceOf(PlanModificationNotAllowedException.class)
                .hasMessage("Plan cannot be modified: " + archived.getId());

        Plan active = repository.save(PlanTestData.trafficLimitedPlan("ACTIVE_PLAN", 2));
        assertThatThrownBy(() -> service.update(new UpdatePlanCommand(
                active.getId(),
                "Invalid",
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

        assertThatNullPointerException()
                .isThrownBy(() -> service.update(null))
                .withMessage("command must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> service.update(validCommand(null)))
                .withMessage("planId must not be null");
    }

    private UpdatePlanCommand validCommand(UUID planId) {
        return new UpdatePlanCommand(
                planId,
                "Updated",
                null,
                PlanType.UNLIMITED,
                0,
                CurrencyCode.IRT,
                30,
                null,
                null,
                1
        );
    }
}
