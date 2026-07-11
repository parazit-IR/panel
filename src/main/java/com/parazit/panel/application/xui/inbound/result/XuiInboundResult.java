package com.parazit.panel.application.xui.inbound.result;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record XuiInboundResult(
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

    public XuiInboundResult {
        shortIds = List.copyOf(Objects.requireNonNull(shortIds, "shortIds must not be null"));
    }
}
