package com.parazit.panel.api.internal.xui.client;

import com.parazit.panel.application.xui.client.command.DeleteVpnClientCommand;
import com.parazit.panel.application.xui.client.command.DisableVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class XuiClientLifecycleApiMapper {

    public DisableVpnClientCommand toCommand(UUID provisionId, DisableXuiClientRequestDto request) {
        return new DisableVpnClientCommand(request.telegramUserId(), provisionId);
    }

    public DeleteVpnClientCommand toCommand(UUID provisionId, DeleteXuiClientRequestDto request) {
        return new DeleteVpnClientCommand(request.telegramUserId(), provisionId, Boolean.TRUE.equals(request.force()));
    }

    public XuiClientLifecycleResponse toResponse(XuiClientLifecycleResult result) {
        return new XuiClientLifecycleResponse(
                result.provisionId(),
                result.inboundId(),
                result.remoteClientId(),
                result.remoteEmail(),
                result.status(),
                result.provisionedAt(),
                result.disabledAt(),
                result.deletedAt(),
                result.changed(),
                result.remoteClientPresent()
        );
    }
}
