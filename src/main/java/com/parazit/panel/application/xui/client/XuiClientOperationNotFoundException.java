package com.parazit.panel.application.xui.client;

import java.util.UUID;

public class XuiClientOperationNotFoundException extends RuntimeException {

    public XuiClientOperationNotFoundException(UUID operationId) {
        super("Xui client operation was not found: " + operationId);
    }
}
