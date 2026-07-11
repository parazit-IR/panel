package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientRemotePayload;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientSettingsRemoteDto;
import com.parazit.panel.infrastructure.xui.dto.client.XuiCreateClientRemoteRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class XuiCreateClientPayloadBuilder {

    public XuiCreateClientRemoteRequest build(CreateXuiClientRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        XuiClientRemotePayload client = new XuiClientRemotePayload(
                request.clientId(),
                request.flow(),
                request.email(),
                request.ipLimit(),
                request.totalTrafficLimitBytes(),
                request.expiryTime().toEpochMilli(),
                request.enabled(),
                "",
                request.subscriptionId(),
                "",
                0
        );
        return new XuiCreateClientRemoteRequest(
                request.inboundId(),
                new XuiClientSettingsRemoteDto(List.of(client))
        );
    }
}
