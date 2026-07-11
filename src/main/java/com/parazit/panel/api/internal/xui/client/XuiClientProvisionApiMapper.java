package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.xui.client.command.CreateVpnClientCommand;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import org.springframework.stereotype.Component;

@Component
public class XuiClientProvisionApiMapper {

    public CreateVpnClientCommand toCommand(CreateXuiClientRequestDto request) {
        return new CreateVpnClientCommand(
                request.telegramUserId(),
                request.planSelectionId(),
                request.inboundId()
        );
    }

    public XuiClientProvisionResponse toResponse(CreateVpnClientResult result) {
        return new XuiClientProvisionResponse(
                result.provisionId(),
                result.planId(),
                result.planSelectionId(),
                result.inboundId(),
                result.remoteClientId(),
                result.remoteEmail(),
                result.status(),
                result.trafficLimitBytes(),
                result.expiresAt(),
                result.ipLimit(),
                result.provisionedAt(),
                result.newlyCreated()
        );
    }
}
