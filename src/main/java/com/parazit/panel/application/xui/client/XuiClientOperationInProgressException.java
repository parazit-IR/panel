package com.parazit.panel.application.xui.client;

public class XuiClientOperationInProgressException extends RuntimeException {

    public XuiClientOperationInProgressException() {
        super("Another Xui client operation is already in progress for this provision");
    }
}
