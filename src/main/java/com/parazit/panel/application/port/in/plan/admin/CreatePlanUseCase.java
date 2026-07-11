package com.parazit.panel.application.port.in.plan.admin;

import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.result.PlanResult;

public interface CreatePlanUseCase {

    PlanResult create(CreatePlanCommand command);
}
