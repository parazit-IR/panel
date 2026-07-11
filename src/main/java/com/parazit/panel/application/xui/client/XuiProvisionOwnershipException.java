package com.parazit.panel.application.xui.client;

import java.util.NoSuchElementException;

public class XuiProvisionOwnershipException extends NoSuchElementException {

    public XuiProvisionOwnershipException() {
        super("Xui client provision not found");
    }
}
