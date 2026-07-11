package com.parazit.panel.application.xui.inbound;

public class XuiInboundAmbiguousException extends RuntimeException {

    public XuiInboundAmbiguousException(String remark) {
        super("Xui inbound remark is ambiguous: " + remark);
    }
}
