package com.parazit.panel.application.port.in.provisioning.outbox;

import com.parazit.panel.application.provisioning.outbox.query.GetProvisioningOutboxQuery;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;

public interface GetProvisioningOutboxUseCase {

    ProvisioningOutboxResult get(GetProvisioningOutboxQuery query);
}
