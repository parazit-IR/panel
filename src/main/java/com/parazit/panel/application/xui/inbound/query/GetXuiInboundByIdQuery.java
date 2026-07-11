package com.parazit.panel.application.xui.inbound.query;

public record GetXuiInboundByIdQuery(long inboundId) {

    public GetXuiInboundByIdQuery {
        if (inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
    }
}
