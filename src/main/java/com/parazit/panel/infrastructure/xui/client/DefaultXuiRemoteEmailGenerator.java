package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.port.out.xui.XuiRemoteEmailGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultXuiRemoteEmailGenerator implements XuiRemoteEmailGenerator {

    @Override
    public String generate(UUID userId, UUID provisionId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(provisionId, "provisionId must not be null");
        return "vpn_" + shortHash(userId.toString()) + "_" + shortHash(provisionId.toString());
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
