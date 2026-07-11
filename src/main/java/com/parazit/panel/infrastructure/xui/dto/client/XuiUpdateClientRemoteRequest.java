package com.parazit.panel.infrastructure.xui.dto.client;

public record XuiUpdateClientRemoteRequest(
        long id,
        XuiClientSettingsRemoteDto settings
) {
}
