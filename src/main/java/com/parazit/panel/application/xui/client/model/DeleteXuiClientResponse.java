package com.parazit.panel.application.xui.client.model;

public record DeleteXuiClientResponse(
        long inboundId,
        String clientId,
        boolean deleted,
        boolean alreadyAbsent,
        String remoteMessage
) {
}
