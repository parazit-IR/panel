package com.parazit.panel.api.internal.provisioning;

import com.parazit.panel.application.provisioning.outbox.query.GetProvisioningOutboxQuery;
import com.parazit.panel.application.provisioning.outbox.query.ListProvisioningOutboxQuery;
import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningOutboxApiMapper {

    public GetProvisioningOutboxQuery toGetQuery(java.util.UUID eventId) {
        return new GetProvisioningOutboxQuery(eventId);
    }

    public ListProvisioningOutboxQuery toListQuery(Integer limit) {
        return new ListProvisioningOutboxQuery(limit == null ? 50 : limit);
    }

    public ProvisioningOutboxResponse toResponse(ProvisioningOutboxResult result) {
        return new ProvisioningOutboxResponse(
                result.eventId(),
                result.orderId(),
                result.paymentId(),
                result.userId(),
                result.planId(),
                result.planSelectionId(),
                result.type(),
                result.status(),
                result.payloadVersion(),
                result.attemptCount(),
                result.availableAt(),
                result.processingStartedAt(),
                result.processedAt(),
                result.lastFailedAt(),
                result.failureCode(),
                result.failureMessage()
        );
    }
}
