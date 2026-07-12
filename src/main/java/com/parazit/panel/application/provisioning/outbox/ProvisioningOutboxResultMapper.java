package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.provisioning.outbox.result.ProvisioningOutboxResult;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningOutboxResultMapper {

    public ProvisioningOutboxResult toResult(ProvisioningOutbox outbox) {
        return new ProvisioningOutboxResult(
                outbox.getEventId(),
                outbox.getOrderId(),
                outbox.getPaymentId(),
                outbox.getUserId(),
                outbox.getPlanId(),
                outbox.getPlanSelectionId(),
                outbox.getType(),
                outbox.getStatus(),
                outbox.getPayloadVersion(),
                outbox.getAttemptCount(),
                outbox.getAvailableAt(),
                outbox.getProcessingStartedAt(),
                outbox.getProcessedAt(),
                outbox.getLastFailedAt(),
                outbox.getFailureCode(),
                outbox.getFailureMessage()
        );
    }
}
