package com.parazit.panel.application.provisioning.outbox.query;

import java.util.Objects;
import java.util.UUID;

public record GetProvisioningOutboxQuery(UUID eventId) {

    public GetProvisioningOutboxQuery {
        Objects.requireNonNull(eventId, "eventId must not be null");
    }
}
