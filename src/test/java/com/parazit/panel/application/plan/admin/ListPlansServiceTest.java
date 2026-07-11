package com.parazit.panel.application.plan.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.parazit.panel.application.plan.PlanResultMapper;
import com.parazit.panel.application.plan.admin.query.ListPlansQuery;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import org.junit.jupiter.api.Test;

class ListPlansServiceTest {

    @Test
    void listsAllPlansUsingDeterministicRepositoryMethod() {
        FakePlanRepository repository = seededRepository();

        assertThat(new ListPlansService(repository, new PlanResultMapper()).list(new ListPlansQuery(null, null)))
                .extracting(result -> result.code())
                .containsExactly("A_LIMITED", "C_LIMITED", "B_UNLIMITED");
        assertThat(repository.findAllOrderByDisplayOrderAscCodeAscCalls).isEqualTo(1);
    }

    @Test
    void listsByStatus() {
        FakePlanRepository repository = seededRepository();

        assertThat(new ListPlansService(repository, new PlanResultMapper()).list(new ListPlansQuery(PlanStatus.DRAFT, null)))
                .extracting(result -> result.code())
                .containsExactly("A_LIMITED", "C_LIMITED", "B_UNLIMITED");
        assertThat(repository.findAllByStatusOrderByDisplayOrderAscCodeAscCalls).isEqualTo(1);
    }

    @Test
    void listsByType() {
        FakePlanRepository repository = seededRepository();

        assertThat(new ListPlansService(repository, new PlanResultMapper()).list(new ListPlansQuery(null, PlanType.UNLIMITED)))
                .extracting(result -> result.code())
                .containsExactly("B_UNLIMITED");
        assertThat(repository.findAllByTypeOrderByDisplayOrderAscCodeAscCalls).isEqualTo(1);
    }

    @Test
    void listsByStatusAndType() {
        FakePlanRepository repository = seededRepository();

        assertThat(new ListPlansService(repository, new PlanResultMapper()).list(new ListPlansQuery(PlanStatus.DRAFT, PlanType.TRAFFIC_LIMITED)))
                .extracting(result -> result.code())
                .containsExactly("A_LIMITED", "C_LIMITED");
        assertThat(repository.findAllByStatusAndTypeOrderByDisplayOrderAscCodeAscCalls).isEqualTo(1);
    }

    @Test
    void returnsEmptyListAndRejectsNullQuery() {
        ListPlansService service = new ListPlansService(new FakePlanRepository(), new PlanResultMapper());

        assertThat(service.list(new ListPlansQuery(PlanStatus.ACTIVE, null))).isEmpty();
        assertThatNullPointerException()
                .isThrownBy(() -> service.list(null))
                .withMessage("query must not be null");
    }

    private FakePlanRepository seededRepository() {
        FakePlanRepository repository = new FakePlanRepository();
        repository.save(PlanTestData.unlimitedPlan("B_UNLIMITED", 2));
        repository.save(PlanTestData.trafficLimitedPlan("C_LIMITED", 1));
        repository.save(PlanTestData.trafficLimitedPlan("A_LIMITED", 1));
        return repository;
    }
}
