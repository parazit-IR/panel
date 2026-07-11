package com.parazit.panel.application.plan.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.query.GetPlanByCodeQuery;
import com.parazit.panel.application.plan.admin.query.GetPlanByIdQuery;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPlanServiceTest {

    @Test
    void getsPlanByIdAndMapsResult() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan plan = repository.save(PlanTestData.trafficLimitedPlan("MONTHLY_30GB", 1));

        PlanResult result = new GetPlanService(repository, new PlanResultMapper())
                .getById(new GetPlanByIdQuery(plan.getId()));

        assertThat(result.id()).isEqualTo(plan.getId());
        assertThat(result.code()).isEqualTo("MONTHLY_30GB");
        assertThat(result.createdAt()).isEqualTo(FakePlanRepository.CREATED_AT);
        assertThat(repository.findByIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(1);
    }

    @Test
    void getsPlanByNormalizedCode() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan plan = repository.save(PlanTestData.unlimitedPlan("MONTHLY_UNLIMITED", 1));

        PlanResult result = new GetPlanService(repository, new PlanResultMapper())
                .getByCode(new GetPlanByCodeQuery(" monthly_unlimited "));

        assertThat(result.id()).isEqualTo(plan.getId());
        assertThat(result.code()).isEqualTo("MONTHLY_UNLIMITED");
        assertThat(repository.findByCodeCalls).isEqualTo(1);
    }

    @Test
    void throwsNotFoundForMissingIdAndCode() {
        FakePlanRepository repository = new FakePlanRepository();
        GetPlanService service = new GetPlanService(repository, new PlanResultMapper());
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertThatThrownBy(() -> service.getById(new GetPlanByIdQuery(missingId)))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found for id " + missingId);
        assertThatThrownBy(() -> service.getByCode(new GetPlanByCodeQuery("MISSING_PLAN")))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage("Plan not found for code MISSING_PLAN");
        assertThat(repository.saveCalls).isZero();
    }

    @Test
    void rejectsNullQueriesAndIds() {
        GetPlanService service = new GetPlanService(new FakePlanRepository(), new PlanResultMapper());

        assertThatNullPointerException()
                .isThrownBy(() -> service.getById(null))
                .withMessage("query must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> service.getById(new GetPlanByIdQuery(null)))
                .withMessage("planId must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> service.getByCode(null))
                .withMessage("query must not be null");
    }
}
