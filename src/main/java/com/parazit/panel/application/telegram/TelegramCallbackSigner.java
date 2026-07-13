package com.parazit.panel.application.telegram;

import com.parazit.panel.config.properties.TelegramBotProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TelegramCallbackSigner {

    private static final String HMAC = "HmacSHA256";
    private final TelegramBotProperties properties;

    public TelegramCallbackSigner(TelegramBotProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public String sign(String unsignedPayload, long telegramUserId) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(properties.callbackSigningSecret().getBytes(StandardCharsets.UTF_8), HMAC));
            byte[] digest = mac.doFinal((telegramUserId + "|" + unsignedPayload).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 10);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Could not sign Telegram callback data", exception);
        }
    }

    public boolean matches(String unsignedPayload, String signature, long telegramUserId) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                sign(unsignedPayload, telegramUserId).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
