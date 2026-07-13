package com.parazit.panel.application.qrcode;

public class QrPayloadTooLargeException extends QrCodeGenerationException {

    public QrPayloadTooLargeException() {
        super("QR payload is too large");
    }
}

