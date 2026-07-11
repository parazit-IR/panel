package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.time.Instant;
import java.util.UUID;

public record XuiClientUpdateResponse(
        UUID operationId,
        UUID provisionId,
        long inboundId,
        String remoteClientId,
        String remoteEmail,
        XuiProvisionStatus provisionStatus,
        XuiClientOperationType operationType,
        XuiClientOperationStatus operationStatus,
        boolean enabled,
        long trafficLimitBytes,
        long uploadBytes,
        long downloadBytes,
        long totalConsumedBytes,
        Long remainingBytes,
        Instant expiresAt,
        int ipLimit,
        Instant synchronizedAt,
        boolean changed
) {
}
