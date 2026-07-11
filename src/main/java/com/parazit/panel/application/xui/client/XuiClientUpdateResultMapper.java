package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class XuiClientUpdateResultMapper {

    public XuiClientUpdateResult toResult(
            XuiClientProvision provision,
            XuiClientOperation operation,
            XuiClientSnapshot remote,
            boolean changed,
            Instant synchronizedAt
    ) {
        long upload = remote == null ? provision.getLastKnownUploadBytes() : remote.uploadBytes();
        long download = remote == null ? provision.getLastKnownDownloadBytes() : remote.downloadBytes();
        long total = Math.addExact(upload, download);
        long limit = remote == null ? provision.getTrafficLimitBytes() : remote.totalTrafficLimitBytes();
        Long remaining = limit == 0 ? null : Math.max(0L, limit - total);
        boolean enabled = remote == null
                ? provision.getStatus() == com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus.ACTIVE
                : remote.enabled();
        return new XuiClientUpdateResult(
                operation.getOperationId(),
                provision.getId(),
                provision.getInboundId(),
                provision.getRemoteClientId(),
                provision.getRemoteEmail(),
                provision.getStatus(),
                operation.getType(),
                operation.getStatus(),
                enabled,
                provision.getTrafficLimitBytes(),
                upload,
                download,
                total,
                remaining,
                provision.getExpiresAt(),
                provision.getIpLimit(),
                synchronizedAt == null ? provision.getLastSynchronizedAt() : synchronizedAt,
                changed
        );
    }
}
