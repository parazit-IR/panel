package com.parazit.panel.application.xui.client.model;

public record DisableXuiClientResponse(
        long inboundId,
        String clientId,
        boolean disabled,
        boolean alreadyDisabled,
        String remoteMessage
) {
}
