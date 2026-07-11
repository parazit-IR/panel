package com.parazit.panel.application.port.in.plan.admin;

import com.parazit.panel.application.plan.admin.command.UpdatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;

public interface UpdatePlanUseCase {

    PlanResult update(UpdatePlanCommand command);
}
