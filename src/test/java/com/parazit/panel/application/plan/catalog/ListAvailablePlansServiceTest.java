package com.parazit.panel.application.plan.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import org.junit.jupiter.api.Test;

class ListAvailablePlansServiceTest {

    @Test
    void listsOnlyActivePlansWithoutFilter() {
        FakePlanRepository repository = seededRepository();

        assertThat(new ListAvailablePlansService(repository, new AvailablePlanResultMapper())
                .list(new ListAvailablePlansQuery(null)))
                .extracting(result -> result.code())
                .containsExactly("A_LIMITED_ACTIVE", "B_UNLIMITED_ACTIVE");
        assertThat(repository.findAllByStatusOrderByDisplayOrderAscCodeAscCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(4);
    }

    @Test
    void filtersActivePlansByType() {
        FakePlanRepository repository = seededRepository();

        assertThat(new ListAvailablePlansService(repository, new AvailablePlanResultMapper())
                .list(new ListAvailablePlansQuery(PlanType.TRAFFIC_LIMITED)))
                .extracting(result -> result.code())
                .containsExactly("A_LIMITED_ACTIVE");
        assertThat(repository.findAllByStatusAndTypeOrderByDisplayOrderAscCodeAscCalls).isEqualTo(1);
    }

    @Test
    void returnsEmptyListAndRejectsNullQuery() {
        ListAvailablePlansService service = new ListAvailablePlansService(
                new FakePlanRepository(),
                new AvailablePlanResultMapper()
        );

        assertThat(service.list(new ListAvailablePlansQuery(PlanType.UNLIMITED))).isEmpty();
        assertThatNullPointerException()
                .isThrownBy(() -> service.list(null))
                .withMessage("query must not be null");
    }

    private FakePlanRepository seededRepository() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan activeLimited = repository.save(PlanTestData.trafficLimitedPlan("A_LIMITED_ACTIVE", 1));
        activeLimited.activate();
        Plan activeUnlimited = repository.save(PlanTestData.unlimitedPlan("B_UNLIMITED_ACTIVE", 2));
        activeUnlimited.activate();
        repository.save(PlanTestData.trafficLimitedPlan("DRAFT_HIDDEN", 0));
        Plan inactive = repository.save(PlanTestData.unlimitedPlan("INACTIVE_HIDDEN", 0));
        inactive.activate();
        inactive.deactivate();
        repository.saveCalls = 4;
        return repository;
    }
}
