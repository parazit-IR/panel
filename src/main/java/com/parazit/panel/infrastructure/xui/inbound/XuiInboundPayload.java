package com.parazit.panel.infrastructure.xui.inbound;

import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import java.util.List;
import java.util.Objects;

record XuiInboundPayload(
        List<XuiClientSnapshot> clients,
        String streamNetwork,
        String securityType,
        String serverName,
        String publicKey,
        String shortId
) {

    XuiInboundPayload {
        clients = List.copyOf(Objects.requireNonNull(clients, "clients must not be null"));
    }
}
