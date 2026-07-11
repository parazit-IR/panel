package com.parazit.panel.application.port.in.plan.admin;

import com.parazit.panel.application.plan.admin.query.ListPlansQuery;
import com.parazit.panel.application.plan.result.PlanResult;
import java.util.List;

public interface ListPlansUseCase {

    List<PlanResult> list(ListPlansQuery query);
}
