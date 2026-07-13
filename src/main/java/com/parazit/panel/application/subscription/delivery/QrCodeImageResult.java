package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.qrcode.model.QrImageFormat;
import com.parazit.panel.application.qrcode.model.QrPayloadType;
import java.util.Objects;

public final class QrCodeImageResult {

    private final byte[] bytes;
    private final String contentType;
    private final String filename;
    private final String etag;
    private final int width;
    private final int height;
    private final QrPayloadType payloadType;
    private final QrImageFormat format;
    private final boolean download;

    public QrCodeImageResult(
            byte[] bytes,
            String contentType,
            String filename,
            String etag,
            int width,
            int height,
            QrPayloadType payloadType,
            QrImageFormat format,
            boolean download
    ) {
        this.bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
        this.contentType = requireText(contentType, "contentType");
        this.filename = requireText(filename, "filename");
        this.etag = requireText(etag, "etag");
        this.width = width;
        this.height = height;
        this.payloadType = Objects.requireNonNull(payloadType, "payloadType must not be null");
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.download = download;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public String contentType() {
        return contentType;
    }

    public String filename() {
        return filename;
    }

    public String etag() {
        return etag;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public QrPayloadType payloadType() {
        return payloadType;
    }

    public QrImageFormat format() {
        return format;
    }

    public boolean download() {
        return download;
    }

    @Override
    public String toString() {
        return "QrCodeImageResult[contentType=" + contentType
                + ", filename=" + filename
                + ", width=" + width
                + ", height=" + height
                + ", payloadType=" + payloadType
                + ", format=" + format
                + "]";
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

