package com.parazit.panel.infrastructure.xui.exception;

public class XuiInvalidResponseException extends XuiException {

    public XuiInvalidResponseException(String message) {
        super(message);
    }

    public XuiInvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
