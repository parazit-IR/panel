package com.parazit.panel.application.xui.client;

import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class XuiClientOperationFingerprint {

    private XuiClientOperationFingerprint() {
    }

    public static String of(UUID provisionId, XuiClientOperationType type, Map<String, ?> values) {
        TreeMap<String, String> normalized = new TreeMap<>();
        normalized.put("provisionId", require(provisionId, "provisionId").toString());
        normalized.put("type", require(type, "type").name());
        for (Map.Entry<String, ?> entry : new TreeMap<>(require(values, "values")).entrySet()) {
            Object value = entry.getValue();
            normalized.put(entry.getKey(), value == null ? "" : value.toString().trim());
        }
        StringBuilder canonical = new StringBuilder();
        normalized.forEach((key, value) -> canonical.append(key).append('=').append(value).append('\n'));
        return sha256Hex(canonical.toString());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
