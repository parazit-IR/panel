package com.parazit.panel.application.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.test.fixture.PlanTestData;
import org.junit.jupiter.api.Test;

class PlanPurchasabilityPolicyTest {

    @Test
    void acceptsActiveWellConfiguredPlanWhenSalesAreEnabled() {
        var plan = PlanTestData.trafficLimitedPlan();
        plan.activate();

        assertThat(new PlanPurchasabilityPolicy(enabledSales()).evaluate(plan))
                .isEqualTo(PlanPurchasability.PURCHASABLE);
    }

    @Test
    void rejectsInactivePlanAndGlobalSalesDisable() {
        var plan = PlanTestData.trafficLimitedPlan();

        assertThat(new PlanPurchasabilityPolicy(enabledSales()).evaluate(plan))
                .isEqualTo(PlanPurchasability.INACTIVE);

        plan.activate();
        assertThat(new PlanPurchasabilityPolicy(disabledSales()).evaluate(plan))
                .isEqualTo(PlanPurchasability.TEMPORARILY_UNAVAILABLE);
    }

    private static SalesAvailabilityService enabledSales() {
        return org.mockito.Mockito.mock(SalesAvailabilityService.class, invocation -> {
            if ("newPurchaseAvailable".equals(invocation.getMethod().getName())) {
                return true;
            }
            return invocation.callRealMethod();
        });
    }

    private static SalesAvailabilityService disabledSales() {
        return org.mockito.Mockito.mock(SalesAvailabilityService.class, invocation -> {
            if ("newPurchaseAvailable".equals(invocation.getMethod().getName())) {
                return false;
            }
            return invocation.callRealMethod();
        });
    }
}
