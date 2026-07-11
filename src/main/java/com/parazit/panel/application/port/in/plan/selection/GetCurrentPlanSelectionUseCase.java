package com.parazit.panel.application.port.in.plan.selection;

import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;

public interface GetCurrentPlanSelectionUseCase {

    PlanSelectionResult getCurrent(GetCurrentPlanSelectionQuery query);
}
