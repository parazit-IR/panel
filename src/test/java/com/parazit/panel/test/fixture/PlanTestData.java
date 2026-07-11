package com.parazit.panel.test.fixture;

import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;

public final class PlanTestData {

    public static final long GIB = 1024L * 1024L * 1024L;
    public static final long THIRTY_GIB = 30L * GIB;

    private PlanTestData() {
    }

    public static Plan trafficLimitedPlan() {
        return trafficLimitedPlan("MONTHLY_30GB", 1);
    }

    public static Plan trafficLimitedPlan(String code, int displayOrder) {
        return Plan.create(
                code,
                "Monthly 30 GiB",
                "30 GiB plan",
                PlanType.TRAFFIC_LIMITED,
                500_000L,
                CurrencyCode.IRT,
                30,
                THIRTY_GIB,
                2,
                displayOrder
        );
    }

    public static Plan unlimitedPlan() {
        return unlimitedPlan("MONTHLY_UNLIMITED", 2);
    }

    public static Plan unlimitedPlan(String code, int displayOrder) {
        return Plan.create(
                code,
                "Monthly Unlimited",
                null,
                PlanType.UNLIMITED,
                900_000L,
                CurrencyCode.IRT,
                30,
                null,
                null,
                displayOrder
        );
    }
}
