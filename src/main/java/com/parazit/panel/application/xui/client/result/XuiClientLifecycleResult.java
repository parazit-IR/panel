package com.parazit.panel.application.xui.client.result;

import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.UUID;

public record XuiClientLifecycleResult(
        UUID provisionId,
        UUID userId,
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
