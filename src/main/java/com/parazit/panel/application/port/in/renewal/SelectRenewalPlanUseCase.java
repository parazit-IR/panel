package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.SelectRenewalPlanCommand;
import com.parazit.panel.application.renewal.result.RenewalSelectionResult;

public interface SelectRenewalPlanUseCase {

    RenewalSelectionResult select(SelectRenewalPlanCommand command);
}
