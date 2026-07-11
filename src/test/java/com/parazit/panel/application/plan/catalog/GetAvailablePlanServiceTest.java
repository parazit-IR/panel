package com.parazit.panel.application.plan.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByCodeQuery;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByIdQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAvailablePlanServiceTest {

    @Test
    void getsActivePlanByIdAndNormalizedCode() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan plan = repository.save(PlanTestData.trafficLimitedPlan("MONTHLY_30GB", 1));
        plan.activate();
        repository.saveCalls = 0;
        GetAvailablePlanService service = new GetAvailablePlanService(repository, new AvailablePlanResultMapper());

        AvailablePlanResult byId = service.getById(new GetAvailablePlanByIdQuery(plan.getId()));
        AvailablePlanResult byCode = service.getByCode(new GetAvailablePlanByCodeQuery(" monthly_30gb "));

        assertThat(byId.id()).isEqualTo(plan.getId());
        assertThat(byCode.id()).isEqualTo(plan.getId());
        assertThat(repository.findByIdAndStatusCalls).isEqualTo(1);
        assertThat(repository.findByCodeAndStatusCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void hiddenPlansBehaveAsNotFound() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan draft = repository.save(PlanTestData.trafficLimitedPlan("DRAFT_HIDDEN", 1));
        Plan inactive = repository.save(PlanTestData.unlimitedPlan("INACTIVE_HIDDEN", 2));
        inactive.activate();
        inactive.deactivate();
        Plan archived = repository.save(PlanTestData.unlimitedPlan("ARCHIVED_HIDDEN", 3));
        archived.archive();
        GetAvailablePlanService service = new GetAvailablePlanService(repository, new AvailablePlanResultMapper());

        assertThatThrownBy(() -> service.getById(new GetAvailablePlanByIdQuery(draft.getId())))
                .isInstanceOf(AvailablePlanNotFoundException.class)
                .hasMessage("Available plan not found for id " + draft.getId());
        assertThatThrownBy(() -> service.getByCode(new GetAvailablePlanByCodeQuery("inactive_hidden")))
                .isInstanceOf(AvailablePlanNotFoundException.class)
                .hasMessage("Available plan not found for code INACTIVE_HIDDEN");
        assertThatThrownBy(() -> service.getById(new GetAvailablePlanByIdQuery(archived.getId())))
                .isInstanceOf(AvailablePlanNotFoundException.class)
                .hasMessage("Available plan not found for id " + archived.getId());
    }

    @Test
    void missingPlansAndNullInputsAreRejected() {
        GetAvailablePlanService service = new GetAvailablePlanService(
                new FakePlanRepository(),
                new AvailablePlanResultMapper()
        );
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThatThrownBy(() -> service.getById(new GetAvailablePlanByIdQuery(missingId)))
                .isInstanceOf(AvailablePlanNotFoundException.class)
                .hasMessage("Available plan not found for id " + missingId);
        assertThatThrownBy(() -> service.getByCode(new GetAvailablePlanByCodeQuery("MISSING_PLAN")))
                .isInstanceOf(AvailablePlanNotFoundException.class)
                .hasMessage("Available plan not found for code MISSING_PLAN");
        assertThatNullPointerException()
                .isThrownBy(() -> service.getById(null))
                .withMessage("query must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> service.getById(new GetAvailablePlanByIdQuery(null)))
                .withMessage("planId must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> service.getByCode(null))
                .withMessage("query must not be null");
    }
}
