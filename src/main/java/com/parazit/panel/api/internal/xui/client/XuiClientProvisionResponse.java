package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.UUID;

public record XuiClientProvisionResponse(
        UUID provisionId,
        UUID planId,
        UUID planSelectionId,
        long inboundId,
        String remoteClientId,
        String remoteEmail,
        XuiProvisionStatus status,
        long trafficLimitBytes,
        Instant expiresAt,
        int ipLimit,
        Instant provisionedAt,
        boolean newlyCreated
) {
}
