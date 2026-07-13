package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.GetRenewalTargetDetailsCommand;
import com.parazit.panel.application.renewal.result.RenewalTargetDetailsResult;

public interface GetRenewalTargetDetailsUseCase {

    RenewalTargetDetailsResult get(GetRenewalTargetDetailsCommand command);
}
