package com.parazit.panel.application.xui.inbound;

public class XuiInboundNotFoundException extends RuntimeException {

    public XuiInboundNotFoundException(long inboundId) {
        super("Xui inbound not found for id " + inboundId);
    }

    public XuiInboundNotFoundException(String remark) {
        super("Xui inbound not found for remark " + remark);
    }
}
