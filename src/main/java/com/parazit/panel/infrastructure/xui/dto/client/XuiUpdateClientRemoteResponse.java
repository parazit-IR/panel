package com.parazit.panel.infrastructure.xui.dto.client;

import com.fasterxml.jackson.databind.JsonNode;

public record XuiUpdateClientRemoteResponse(
        Boolean success,
        String msg,
        JsonNode obj
) {
}
