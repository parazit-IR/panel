package com.parazit.panel.application.provisioning.outbox;

public class ProvisioningOutboxRetryNotAllowedException extends RuntimeException {

    public ProvisioningOutboxRetryNotAllowedException(String message) {
        super(message);
    }
}
