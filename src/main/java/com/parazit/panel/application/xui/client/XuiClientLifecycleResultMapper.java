package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class XuiClientLifecycleResultMapper {

    public XuiClientLifecycleResult toResult(
            XuiClientProvision provision,
            boolean changed,
            boolean remoteClientPresent
    ) {
        Objects.requireNonNull(provision, "provision must not be null");
        return new XuiClientLifecycleResult(
                provision.getId(),
                provision.getUserId(),
                provision.getInboundId(),
                provision.getRemoteClientId(),
                provision.getRemoteEmail(),
                provision.getStatus(),
                provision.getProvisionedAt(),
                provision.getDisabledAt(),
                provision.getDeletedAt(),
                changed,
                remoteClientPresent
        );
    }
}
