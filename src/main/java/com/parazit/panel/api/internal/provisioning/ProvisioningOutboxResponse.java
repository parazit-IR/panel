package com.parazit.panel.api.internal.provisioning;

import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxStatus;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import java.time.Instant;
import java.util.UUID;

public record ProvisioningOutboxResponse(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        UUID userId,
        UUID planId,
        UUID planSelectionId,
        ProvisioningOutboxType type,
        ProvisioningOutboxStatus status,
        String payloadVersion,
        int attemptCount,
        Instant availableAt,
        Instant processingStartedAt,
        Instant processedAt,
        Instant lastFailedAt,
        String failureCode,
        String failureMessage
) {
}
