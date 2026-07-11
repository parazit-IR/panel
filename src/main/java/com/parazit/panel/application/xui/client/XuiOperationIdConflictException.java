package com.parazit.panel.application.xui.client;

public class XuiOperationIdConflictException extends RuntimeException {

    public XuiOperationIdConflictException() {
        super("Xui client operation id was already used for a different request");
    }
}
