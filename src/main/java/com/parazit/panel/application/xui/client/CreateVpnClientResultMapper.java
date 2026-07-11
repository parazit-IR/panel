package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import org.springframework.stereotype.Component;

@Component
public class CreateVpnClientResultMapper {

    public CreateVpnClientResult toResult(XuiClientProvision provision, boolean newlyCreated) {
        return new CreateVpnClientResult(
                provision.getId(),
                provision.getUserId(),
                provision.getPlanId(),
                provision.getPlanSelectionId(),
                provision.getInboundId(),
                provision.getRemoteClientId(),
                provision.getRemoteEmail(),
                provision.getStatus(),
                provision.getTrafficLimitBytes(),
                provision.getExpiresAt(),
                provision.getIpLimit(),
                provision.getProvisionedAt(),
                newlyCreated
        );
    }
}
