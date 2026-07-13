package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.CreateRenewalOrderCommand;
import com.parazit.panel.application.renewal.result.CreateRenewalOrderResult;

public interface CreateRenewalOrderUseCase {

    CreateRenewalOrderResult create(CreateRenewalOrderCommand command);
}
