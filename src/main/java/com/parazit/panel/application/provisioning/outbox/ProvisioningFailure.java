package com.parazit.panel.application.provisioning.outbox;

public record ProvisioningFailure(
        boolean retryable,
        boolean unknown,
        String code,
        String message
) {
}
