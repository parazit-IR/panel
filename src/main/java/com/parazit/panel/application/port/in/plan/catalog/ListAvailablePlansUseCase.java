package com.parazit.panel.application.port.in.plan.catalog;

import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import java.util.List;

public interface ListAvailablePlansUseCase {

    List<AvailablePlanResult> list(ListAvailablePlansQuery query);
}
