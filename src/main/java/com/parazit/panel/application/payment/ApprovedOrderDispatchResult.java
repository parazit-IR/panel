package com.parazit.panel.application.payment;

import java.util.UUID;

public record ApprovedOrderDispatchResult(
        UUID provisioningEventId,
        boolean provisioningRequired
) {

    public static ApprovedOrderDispatchResult none() {
        return new ApprovedOrderDispatchResult(null, false);
    }

    public static ApprovedOrderDispatchResult provisioning(UUID provisioningEventId) {
        return new ApprovedOrderDispatchResult(provisioningEventId, true);
    }
}
