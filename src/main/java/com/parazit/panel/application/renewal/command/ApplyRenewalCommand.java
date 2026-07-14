package com.parazit.panel.application.renewal.command;

import java.util.Objects;
import java.util.UUID;

public record ApplyRenewalCommand(
        UUID renewalOutboxId,
        UUID renewalOrderId,
        UUID targetSubscriptionId,
        UUID targetProvisionId,
        UUID executionRequestId
) {

    public ApplyRenewalCommand {
        renewalOutboxId = Objects.requireNonNull(renewalOutboxId, "renewalOutboxId must not be null");
        renewalOrderId = Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null");
        targetSubscriptionId = Objects.requireNonNull(targetSubscriptionId, "targetSubscriptionId must not be null");
        targetProvisionId = Objects.requireNonNull(targetProvisionId, "targetProvisionId must not be null");
        executionRequestId = Objects.requireNonNull(executionRequestId, "executionRequestId must not be null");
    }
}
