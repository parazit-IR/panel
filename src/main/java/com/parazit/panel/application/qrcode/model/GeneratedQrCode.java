package com.parazit.panel.application.qrcode.model;

import java.util.Arrays;
import java.util.Objects;

public final class GeneratedQrCode {

    private final byte[] bytes;
    private final QrImageFormat format;
    private final String contentType;
    private final int width;
    private final int height;
    private final String contentHash;

    public GeneratedQrCode(
            byte[] bytes,
            QrImageFormat format,
            String contentType,
            int width,
            int height,
            String contentHash
    ) {
        this.bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
        if (this.bytes.length == 0) {
            throw new IllegalArgumentException("QR bytes must not be empty");
        }
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.contentType = requireText(contentType, "contentType");
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("QR dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.contentHash = requireText(contentHash, "contentHash");
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public QrImageFormat format() {
        return format;
    }

    public String contentType() {
        return contentType;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String contentHash() {
        return contentHash;
    }

    @Override
    public String toString() {
        return "GeneratedQrCode[format=" + format
                + ", contentType=" + contentType
                + ", width=" + width
                + ", height=" + height
                + ", bytes=" + bytes.length
                + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GeneratedQrCode that)) {
            return false;
        }
        return width == that.width
                && height == that.height
                && Arrays.equals(bytes, that.bytes)
                && format == that.format
                && contentType.equals(that.contentType)
                && contentHash.equals(that.contentHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(format, contentType, width, height, contentHash);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

