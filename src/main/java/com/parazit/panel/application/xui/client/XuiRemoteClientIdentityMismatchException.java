package com.parazit.panel.application.xui.client;

public class XuiRemoteClientIdentityMismatchException extends RuntimeException {

    public XuiRemoteClientIdentityMismatchException() {
        super("Remote Xui client identity does not match local provision");
    }
}
