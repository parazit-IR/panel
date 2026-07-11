package com.parazit.panel.application.xui.client;

import java.util.NoSuchElementException;
import java.util.UUID;

public class XuiClientProvisionNotFoundException extends NoSuchElementException {

    public XuiClientProvisionNotFoundException(UUID provisionId) {
        super("Xui client provision not found: " + provisionId);
    }
}
