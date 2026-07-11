package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.xui.client.model.DisableXuiClientRequest;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientRemotePayload;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientSettingsRemoteDto;
import com.parazit.panel.infrastructure.xui.dto.client.XuiUpdateClientRemoteRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class XuiDisableClientPayloadBuilder {

    public XuiUpdateClientRemoteRequest build(DisableXuiClientRequest request, XuiClientSnapshot remoteClient) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(remoteClient, "remoteClient must not be null");
        XuiClientRemotePayload client = new XuiClientRemotePayload(
                request.clientId(),
                remoteClient.flow() == null ? "" : remoteClient.flow(),
                request.email(),
                remoteClient.ipLimit(),
                remoteClient.totalTrafficLimitBytes(),
                remoteClient.expiryTime() == null ? 0 : remoteClient.expiryTime().toEpochMilli(),
                false,
                remoteClient.telegramId() == null ? "" : remoteClient.telegramId(),
                remoteClient.subscriptionId(),
                remoteClient.comment() == null ? "" : remoteClient.comment(),
                remoteClient.reset()
        );
        return new XuiUpdateClientRemoteRequest(
                request.inboundId(),
                new XuiClientSettingsRemoteDto(List.of(client))
        );
    }
}
