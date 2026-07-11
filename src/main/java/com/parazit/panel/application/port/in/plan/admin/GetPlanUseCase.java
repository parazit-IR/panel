package com.parazit.panel.application.port.in.plan.admin;

import com.parazit.panel.application.plan.admin.query.GetPlanByCodeQuery;
import com.parazit.panel.application.plan.admin.query.GetPlanByIdQuery;
import com.parazit.panel.application.plan.result.PlanResult;

public interface GetPlanUseCase {

    PlanResult getById(GetPlanByIdQuery query);

    PlanResult getByCode(GetPlanByCodeQuery query);
}
