package com.parazit.panel.infrastructure.xui.dto.client;

public record XuiClientTrafficRemoteDto(
        Boolean success,
        String msg,
        ClientTrafficObj obj
) {

    public record ClientTrafficObj(
            String email,
            Long up,
            Long down,
            Long total,
            Long expiryTime,
            Boolean enable
    ) {
    }
}
