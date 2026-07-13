package com.parazit.panel.application.qrcode.model;

import java.util.Objects;

public record QrRenderOptions(
        int width,
        int height,
        int marginModules,
        QrErrorCorrection errorCorrection,
        QrImageFormat format,
        boolean transparentBackground
) {

    public QrRenderOptions {
        errorCorrection = errorCorrection == null ? QrErrorCorrection.MEDIUM : errorCorrection;
        format = format == null ? QrImageFormat.PNG : format;
        if (width != height) {
            throw new IllegalArgumentException("QR width and height must be equal");
        }
        if (width < 1) {
            throw new IllegalArgumentException("QR size must be positive");
        }
        if (marginModules < 0) {
            throw new IllegalArgumentException("QR margin must not be negative");
        }
        Objects.requireNonNull(errorCorrection, "errorCorrection must not be null");
        Objects.requireNonNull(format, "format must not be null");
    }

    @Override
    public String toString() {
        return "QrRenderOptions[width=" + width
                + ", height=" + height
                + ", marginModules=" + marginModules
                + ", errorCorrection=" + errorCorrection
                + ", format=" + format
                + ", transparentBackground=" + transparentBackground
                + "]";
    }
}

