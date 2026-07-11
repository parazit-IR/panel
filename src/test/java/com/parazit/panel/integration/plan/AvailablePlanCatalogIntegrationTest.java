package com.parazit.panel.integration.plan;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByCodeQuery;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByIdQuery;
import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.port.in.plan.catalog.GetAvailablePlanUseCase;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AvailablePlanCatalogIntegrationTest extends PostgreSqlContainerSupport {

    private final ListAvailablePlansUseCase listAvailablePlansUseCase;
    private final GetAvailablePlanUseCase getAvailablePlanUseCase;
    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;

    AvailablePlanCatalogIntegrationTest(
            ListAvailablePlansUseCase listAvailablePlansUseCase,
            GetAvailablePlanUseCase getAvailablePlanUseCase,
            PlanRepository planRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.listAvailablePlansUseCase = listAvailablePlansUseCase;
        this.getAvailablePlanUseCase = getAvailablePlanUseCase;
        this.planRepository = planRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
    }

    @Test
    void catalogListsAndGetsOnlyActivePlansWithoutMutatingAudits() {
        Plan draft = planRepository.save(PlanTestData.trafficLimitedPlan("DRAFT_HIDDEN", 0));
        Plan activeLimited = activate(planRepository.save(PlanTestData.trafficLimitedPlan("A_LIMITED_ACTIVE", 1)));
        Plan activeUnlimited = activate(planRepository.save(PlanTestData.unlimitedPlan("B_UNLIMITED_ACTIVE", 2)));
        Plan inactive = activate(planRepository.save(PlanTestData.unlimitedPlan("INACTIVE_HIDDEN", 3)));
        inactive.deactivate();
        planRepository.save(inactive);
        Plan archived = planRepository.save(PlanTestData.unlimitedPlan("ARCHIVED_HIDDEN", 4));
        archived.archive();
        planRepository.save(archived);

        Instant updatedAtBeforeRead = planRepository.findByCode("A_LIMITED_ACTIVE").orElseThrow().getUpdatedAt();

        assertThat(listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null)))
                .extracting(AvailablePlanResult::code)
                .containsExactly("A_LIMITED_ACTIVE", "B_UNLIMITED_ACTIVE");
        assertThat(listAvailablePlansUseCase.list(new ListAvailablePlansQuery(PlanType.TRAFFIC_LIMITED)))
                .extracting(AvailablePlanResult::code)
                .containsExactly("A_LIMITED_ACTIVE");
        assertThat(getAvailablePlanUseCase.getById(new GetAvailablePlanByIdQuery(activeLimited.getId())).code())
                .isEqualTo("A_LIMITED_ACTIVE");
        assertThat(getAvailablePlanUseCase.getByCode(new GetAvailablePlanByCodeQuery("b_unlimited_active")).id())
                .isEqualTo(activeUnlimited.getId());

        assertThatThrownBy(() -> getAvailablePlanUseCase.getById(new GetAvailablePlanByIdQuery(draft.getId())))
                .isInstanceOf(AvailablePlanNotFoundException.class);
        assertThatThrownBy(() -> getAvailablePlanUseCase.getByCode(new GetAvailablePlanByCodeQuery("inactive_hidden")))
                .isInstanceOf(AvailablePlanNotFoundException.class);
        assertThatThrownBy(() -> getAvailablePlanUseCase.getById(new GetAvailablePlanByIdQuery(archived.getId())))
                .isInstanceOf(AvailablePlanNotFoundException.class);

        assertThat(planRepository.findByCode("A_LIMITED_ACTIVE").orElseThrow().getUpdatedAt())
                .isEqualTo(updatedAtBeforeRead);
    }

    private Plan activate(Plan plan) {
        plan.activate();
        return planRepository.save(plan);
    }
}
