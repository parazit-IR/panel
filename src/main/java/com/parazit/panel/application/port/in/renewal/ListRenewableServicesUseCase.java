package com.parazit.panel.application.port.in.renewal;

import com.parazit.panel.application.renewal.command.ListRenewableServicesCommand;
import com.parazit.panel.application.renewal.result.RenewableServicePageResult;

public interface ListRenewableServicesUseCase {

    RenewableServicePageResult list(ListRenewableServicesCommand command);
}
