package com.parazit.panel.api.internal.xui.inbound;

import java.time.Instant;
import java.util.List;

public record XuiInboundResponse(
        long id,
        String remark,
        String protocol,
        int port,
        boolean enabled,
        String listenAddress,
        long totalTrafficLimitBytes,
        long uploadBytes,
        long downloadBytes,
        Instant expiryTime,
        int clientCount,
        String streamNetwork,
        String securityType,
        String serverName,
        String publicKey,
        List<String> shortIds
) {
}
