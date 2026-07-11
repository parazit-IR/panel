package com.parazit.panel.application.port.in.plan.admin;

import com.parazit.panel.application.plan.admin.command.ChangePlanStatusCommand;
import com.parazit.panel.application.plan.result.PlanResult;

public interface ChangePlanStatusUseCase {

    PlanResult activate(ChangePlanStatusCommand command);

    PlanResult deactivate(ChangePlanStatusCommand command);

    PlanResult archive(ChangePlanStatusCommand command);
}
