package com.parazit.panel.application.xui.client;

public class XuiRemoteClientMissingException extends RuntimeException {

    public XuiRemoteClientMissingException() {
        super("Remote Xui client is missing");
    }
}
