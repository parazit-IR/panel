package com.parazit.panel.application.xui.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record XuiInboundSnapshot(
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
        List<XuiClientSnapshot> clients,
        String streamNetwork,
        String securityType,
        String serverName,
        String publicKey,
        String shortId
) {

    public XuiInboundSnapshot {
        clients = List.copyOf(Objects.requireNonNull(clients, "clients must not be null"));
    }
}
