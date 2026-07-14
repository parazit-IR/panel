package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.ApplyRenewalCommand;
import com.parazit.panel.application.renewal.result.ApplyRenewalResult;

public interface ApplyRenewalUseCase {

    ApplyRenewalResult apply(ApplyRenewalCommand command);
}
