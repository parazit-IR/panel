package com.parazit.panel.application.xui.client;

public class XuiInboundNotEligibleException extends RuntimeException {

    public XuiInboundNotEligibleException(long inboundId) {
        super("Xui inbound is not eligible for provisioning: " + inboundId);
    }
}
