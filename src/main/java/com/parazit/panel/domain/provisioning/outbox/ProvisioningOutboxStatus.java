package com.parazit.panel.domain.provisioning.outbox;

public enum ProvisioningOutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    UNKNOWN,
    DEAD
}
