package com.parazit.panel.infrastructure.xui.dto.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;

public record XuiInboundRemoteDto(
        Long id,
        Long up,
        Long down,
        Long total,
        String remark,
        @JsonAlias({"enable", "enabled"})
        Boolean enabled,
        Long expiryTime,
        String listen,
        Integer port,
        String protocol,
        String settings,
        String streamSettings
) {
}
