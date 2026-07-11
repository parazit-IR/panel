package com.parazit.panel.infrastructure.xui.dto.inbound;

import java.util.List;

public record XuiInboundListResponse(
        Boolean success,
        String msg,
        List<XuiInboundRemoteDto> obj
) {
}
