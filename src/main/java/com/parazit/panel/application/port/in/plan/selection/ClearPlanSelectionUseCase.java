package com.parazit.panel.application.port.in.plan.selection;

import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;

public interface ClearPlanSelectionUseCase {

    PlanSelectionResult clear(ClearPlanSelectionCommand command);
}
