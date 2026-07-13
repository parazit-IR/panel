package com.parazit.panel.application.qrcode;

public class QrRenderingDisabledException extends QrCodeGenerationException {

    public QrRenderingDisabledException() {
        super("QR rendering is disabled");
    }
}

