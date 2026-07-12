package com.parazit.panel.application.provisioning.outbox.command;

import java.util.Objects;
import java.util.UUID;

public record RetryProvisioningOutboxCommand(UUID eventId) {

    public RetryProvisioningOutboxCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
    }
}
