package com.parazit.panel.application.port.in.provisioning.outbox;

import com.parazit.panel.application.provisioning.outbox.query.ListProvisioningOutboxQuery;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;
import java.util.List;

public interface ListProvisioningOutboxUseCase {

    List<ProvisioningOutboxResult> list(ListProvisioningOutboxQuery query);
}
