package com.parazit.panel.infrastructure.xui.dto.client;

public record XuiCreateClientRemoteRequest(
        long id,
        XuiClientSettingsRemoteDto settings
) {
}
