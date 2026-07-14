package com.parazit.panel.infrastructure.security.promotion;

import com.parazit.panel.application.port.out.promotion.PromotionCodeHasher;
import com.parazit.panel.config.properties.PromotionProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacPromotionCodeHasher implements PromotionCodeHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PromotionProperties properties;

    public HmacPromotionCodeHasher(PromotionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public String hashNormalizedCode(String normalizedCode) {
        String code = Objects.requireNonNull(normalizedCode, "normalizedCode must not be null");
        byte[] digest = properties.hashSecret().isBlank()
                ? sha256(code)
                : hmac(code, properties.hashSecret());
        return HexFormat.of().formatHex(digest);
    }

    private static byte[] hmac(String code, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("promotion code hash failed", exception);
        }
    }

    private static byte[] sha256(String code) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
