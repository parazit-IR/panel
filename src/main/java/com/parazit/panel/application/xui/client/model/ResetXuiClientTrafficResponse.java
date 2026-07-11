package com.parazit.panel.application.xui.client.model;

public record ResetXuiClientTrafficResponse(
        long inboundId,
        String clientId,
        String email,
        boolean reset,
        boolean alreadyZero,
        String remoteMessage
) {
}
