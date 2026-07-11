package com.parazit.panel.application.port.in.plan.selection;

import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;

public interface SelectPlanUseCase {

    PlanSelectionResult select(SelectPlanCommand command);
}
