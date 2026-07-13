package com.parazit.panel.application.qrcode;

public class UnsupportedQrFormatException extends QrCodeGenerationException {

    public UnsupportedQrFormatException() {
        super("Unsupported QR image format");
    }
}

