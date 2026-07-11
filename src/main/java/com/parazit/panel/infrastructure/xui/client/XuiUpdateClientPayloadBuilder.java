package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.xui.client.model.UpdateXuiClientRequest;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientRemotePayload;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientSettingsRemoteDto;
import com.parazit.panel.infrastructure.xui.dto.client.XuiUpdateClientRemoteRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class XuiUpdateClientPayloadBuilder {

    public XuiUpdateClientRemoteRequest build(UpdateXuiClientRequest request, XuiClientSnapshot current) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(current, "current must not be null");
        XuiClientRemotePayload client = new XuiClientRemotePayload(
                current.clientId(),
                current.flow() == null ? "" : current.flow(),
                request.newEmail() == null || request.newEmail().isBlank() ? current.email() : request.newEmail().trim(),
                request.ipLimit() == null ? current.ipLimit() : request.ipLimit(),
                request.totalTrafficLimitBytes() == null ? current.totalTrafficLimitBytes() : request.totalTrafficLimitBytes(),
                request.expiryTime() == null ? expiryMillis(current) : request.expiryTime().toEpochMilli(),
                request.enabled() == null ? current.enabled() : request.enabled(),
                current.telegramId() == null ? "" : current.telegramId(),
                current.subscriptionId(),
                current.comment() == null ? "" : current.comment(),
                current.reset()
        );
        return new XuiUpdateClientRemoteRequest(
                request.inboundId(),
                new XuiClientSettingsRemoteDto(List.of(client))
        );
    }

    private static long expiryMillis(XuiClientSnapshot current) {
        return current.expiryTime() == null ? 0 : current.expiryTime().toEpochMilli();
    }
}
