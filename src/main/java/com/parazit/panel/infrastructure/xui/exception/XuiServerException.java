package com.parazit.panel.infrastructure.xui.exception;

public class XuiServerException extends XuiException {

    private final int statusCode;

    public XuiServerException(int statusCode) {
        super("Xui server returned status " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
