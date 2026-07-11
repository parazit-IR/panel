package com.parazit.panel.infrastructure.xui.dto.inbound;

public record XuiApiResponse<T>(
        Boolean success,
        String msg,
        T obj
) {
}
