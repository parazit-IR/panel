package com.parazit.panel.infrastructure.xui.dto.client;

import java.util.List;

public record XuiClientSettingsRemoteDto(
        List<XuiClientRemotePayload> clients
) {
}
