package com.parazit.panel.infrastructure.xui.dto.inbound;

import com.fasterxml.jackson.annotation.JsonAlias;

public record XuiClientRemoteDto(
        @JsonAlias({"id", "clientId"})
        String clientId,
        String email,
        @JsonAlias({"enable", "enabled"})
        Boolean enabled,
        @JsonAlias({"totalGB", "total"})
        Long totalTrafficLimitBytes,
        @JsonAlias({"up", "upload"})
        Long uploadBytes,
        @JsonAlias({"down", "download"})
        Long downloadBytes,
        Long expiryTime,
        @JsonAlias({"limitIp", "ipLimit"})
        Integer ipLimit,
        @JsonAlias({"subId", "subscriptionId"})
        String subscriptionId
) {
}
