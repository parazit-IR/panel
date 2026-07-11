package com.parazit.panel.application.port.in.plan.catalog;

import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByCodeQuery;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByIdQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;

public interface GetAvailablePlanUseCase {

    AvailablePlanResult getById(GetAvailablePlanByIdQuery query);

    AvailablePlanResult getByCode(GetAvailablePlanByCodeQuery query);
}
