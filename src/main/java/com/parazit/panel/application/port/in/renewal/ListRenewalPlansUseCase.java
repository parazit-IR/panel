package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.ListRenewalPlansCommand;
import com.parazit.panel.application.renewal.result.RenewalPlanPageResult;

public interface ListRenewalPlansUseCase {

    RenewalPlanPageResult list(ListRenewalPlansCommand command);
}
