package com.parazit.panel.infrastructure.xui.dto.client;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record XuiClientRemotePayload(
        String id,
        String flow,
        String email,
        int limitIp,
        long totalGB,
        long expiryTime,
        boolean enable,
        String tgId,
        String subId,
        String comment,
        int reset
) {
}
