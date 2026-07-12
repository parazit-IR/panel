package com.parazit.panel.application.port.in.provisioning.outbox;

import com.parazit.panel.application.provisioning.outbox.command.RetryProvisioningOutboxCommand;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;

public interface RetryProvisioningOutboxUseCase {

    ProvisioningOutboxResult retry(RetryProvisioningOutboxCommand command);
}
