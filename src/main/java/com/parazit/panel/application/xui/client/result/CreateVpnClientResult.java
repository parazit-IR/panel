package com.parazit.panel.application.xui.client.result;

import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.UUID;

public record CreateVpnClientResult(
        UUID provisionId,
        UUID userId,
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
