package com.parazit.panel.application.plan.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.FakePlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import org.junit.jupiter.api.Test;

class AvailablePlanResultMapperTest {

    @Test
    void mapsLimitedPlanCanonicalFieldsOnly() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan plan = repository.save(PlanTestData.trafficLimitedPlan("MONTHLY_30GB", 3));
        plan.activate();

        AvailablePlanResult result = new AvailablePlanResultMapper().toResult(plan);

        assertThat(result.id()).isEqualTo(plan.getId());
        assertThat(result.code()).isEqualTo("MONTHLY_30GB");
        assertThat(result.name()).isEqualTo("Monthly 30 GiB");
        assertThat(result.description()).isEqualTo("30 GiB plan");
        assertThat(result.type()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(result.priceAmount()).isEqualTo(500_000L);
        assertThat(result.currency()).isEqualTo(CurrencyCode.IRT);
        assertThat(result.durationDays()).isEqualTo(30);
        assertThat(result.trafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(result.maxDevices()).isEqualTo(2);
        assertThat(result.displayOrder()).isEqualTo(3);
    }

    @Test
    void mapsUnlimitedPlanAndPreservesNullOptionals() {
        FakePlanRepository repository = new FakePlanRepository();
        Plan plan = repository.save(PlanTestData.unlimitedPlan("MONTHLY_UNLIMITED", 2));
        plan.activate();

        AvailablePlanResult result = new AvailablePlanResultMapper().toResult(plan);

        assertThat(result.type()).isEqualTo(PlanType.UNLIMITED);
        assertThat(result.description()).isNull();
        assertThat(result.trafficLimitBytes()).isNull();
        assertThat(result.maxDevices()).isNull();
    }
}
