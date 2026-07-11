package com.parazit.panel.application.xui.inbound.query;

public record GetXuiInboundByRemarkQuery(String remark) {

    public GetXuiInboundByRemarkQuery {
        if (remark == null || remark.isBlank()) {
            throw new IllegalArgumentException("remark must not be blank");
        }
        remark = remark.trim();
    }
}
