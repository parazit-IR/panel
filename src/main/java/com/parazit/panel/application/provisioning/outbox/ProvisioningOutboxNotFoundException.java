package com.parazit.panel.application.provisioning.outbox;

import java.util.UUID;

public class ProvisioningOutboxNotFoundException extends RuntimeException {

    public ProvisioningOutboxNotFoundException(UUID eventId) {
        super("Provisioning outbox event was not found: " + eventId);
    }
}
