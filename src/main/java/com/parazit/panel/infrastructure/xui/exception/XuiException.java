package com.parazit.panel.infrastructure.xui.exception;

public class XuiException extends RuntimeException {

    public XuiException(String message) {
        super(message);
    }

    public XuiException(String message, Throwable cause) {
        super(message, cause);
    }
}
