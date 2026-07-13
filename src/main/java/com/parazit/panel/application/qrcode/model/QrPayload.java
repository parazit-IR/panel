package com.parazit.panel.application.qrcode.model;

import java.util.Objects;

public record QrPayload(
        QrPayloadType type,
        String value
) {

    public QrPayload {
        type = Objects.requireNonNull(type, "type must not be null");
        value = normalize(value);
        validateByType(type, value);
    }

    public void validateLength(int maxPayloadCharacters) {
        if (maxPayloadCharacters < 1) {
            throw new IllegalArgumentException("maxPayloadCharacters must be positive");
        }
        if (value.length() > maxPayloadCharacters) {
            throw new IllegalArgumentException("QR payload is too large");
        }
    }

    @Override
    public String toString() {
        return "QrPayload[type=" + type + ", length=" + value.length() + "]";
    }

    private static String normalize(String value) {
        String normalized = Objects.requireNonNull(value, "value must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("QR payload must not be blank");
        }
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (Character.isISOControl(current)) {
                throw new IllegalArgumentException("QR payload contains control characters");
            }
        }
        if (normalized.contains("\r") || normalized.contains("\n")) {
            throw new IllegalArgumentException("QR payload contains line breaks");
        }
        return normalized;
    }

    private static void validateByType(QrPayloadType type, String value) {
        if (type == QrPayloadType.SUBSCRIPTION_URL) {
            if (!value.startsWith("https://") && !value.startsWith("http://")) {
                throw new IllegalArgumentException("Subscription QR payload must be an absolute URL");
            }
            if (!value.contains("/sub/sub_")) {
                throw new IllegalArgumentException("Subscription QR payload must target a subscription URL");
            }
            return;
        }
        if (type == QrPayloadType.VLESS_URI && !value.startsWith("vless://")) {
            throw new IllegalArgumentException("VLESS QR payload must be a VLESS URI");
        }
    }
}

