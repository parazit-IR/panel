package com.parazit.panel.infrastructure.xui.exception;

public class XuiClientException extends XuiException {

    private final int statusCode;

    public XuiClientException(int statusCode) {
        super("Xui request was rejected with status " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
