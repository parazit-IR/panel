package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.GetRenewalStatusCommand;
import com.parazit.panel.application.renewal.result.RenewalStatusResult;

public interface GetRenewalStatusUseCase {

    RenewalStatusResult get(GetRenewalStatusCommand command);
}
