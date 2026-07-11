package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.UUID;

public record XuiClientLifecycleResponse(
        UUID provisionId,
        long inboundId,
        String remoteClientId,
        String remoteEmail,
        XuiProvisionStatus status,
        Instant provisionedAt,
        Instant disabledAt,
        Instant deletedAt,
        boolean changed,
        boolean remoteClientPresent
) {
}
