package com.parazit.panel.application.xui.client;

public class XuiTrafficOverflowException extends RuntimeException {

    public XuiTrafficOverflowException() {
        super("Xui traffic limit calculation overflowed");
    }
}
