package com.parazit.panel.infrastructure.security;

import com.parazit.panel.application.port.out.security.SubscriptionTokenHasher;
import com.parazit.panel.domain.subscription.SubscriptionAccessToken;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class Sha256SubscriptionTokenHasher implements SubscriptionTokenHasher {

    @Override
    public String hash(String rawToken) {
        String normalized = SubscriptionAccessToken.normalize(rawToken);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Override
    public boolean matches(String rawToken, String expectedHash) {
        String actualHash = hash(rawToken);
        byte[] actual = actualHash.getBytes(StandardCharsets.US_ASCII);
        byte[] expected = Objects.requireNonNull(expectedHash, "expectedHash must not be null")
                .getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
