package com.parazit.panel.application.qrcode;

public class QrCodeGenerationException extends RuntimeException {

    public QrCodeGenerationException(String message) {
        super(message);
    }

    public QrCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

