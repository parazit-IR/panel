package com.parazit.panel.application.xui.client.model;

public record CreateXuiClientResponse(
        long inboundId,
        String clientId,
        String email,
        String subscriptionId,
        boolean created,
        String remoteMessage
) {
}
