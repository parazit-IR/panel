package com.parazit.panel.application.telegram;

import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramCallbackPayload;
import com.parazit.panel.config.properties.TelegramBotProperties;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TelegramCallbackDataCodec {

    private static final String VERSION = "1";

    private final TelegramCallbackSigner signer;
    private final TelegramBotProperties properties;

    public TelegramCallbackDataCodec(TelegramCallbackSigner signer, TelegramBotProperties properties) {
        this.signer = Objects.requireNonNull(signer, "signer must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public String encode(TelegramCallbackPayload payload, long telegramUserId) {
        Objects.requireNonNull(payload, "payload must not be null");
        String unsigned = String.join(":",
                VERSION,
                payload.action().code(),
                encodeUuid(payload.subscriptionId()),
                payload.configIndex() == null ? "" : payload.configIndex().toString(),
                encodeUuid(payload.actionId()),
                payload.reference(),
                Long.toString(payload.expiresAt().getEpochSecond(), 36)
        );
        String encoded = unsigned + ":" + signer.sign(unsigned, telegramUserId);
        if (encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.maxCallbackDataBytes()) {
            throw new IllegalArgumentException("Telegram callback data exceeds configured limit");
        }
        return encoded;
    }

    public TelegramCallbackPayload decode(String callbackData, long telegramUserId, Instant now) {
        if (callbackData == null || callbackData.isBlank()) {
            throw new IllegalArgumentException("callback data is blank");
        }
        String[] parts = callbackData.split(":", -1);
        if ((parts.length != 7 && parts.length != 8) || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("unsupported callback data");
        }
        boolean legacy = parts.length == 7;
        String unsigned = legacy
                ? String.join(":", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
                : String.join(":", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
        String signature = legacy ? parts[6] : parts[7];
        if (!signer.matches(unsigned, signature, telegramUserId)) {
            throw new IllegalArgumentException("callback signature mismatch");
        }
        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(legacy ? parts[5] : parts[6], 36));
        if (!now.isBefore(expiresAt)) {
            throw new IllegalArgumentException("callback data expired");
        }
        Integer index = parts[3].isBlank() ? null : Integer.valueOf(parts[3]);
        return new TelegramCallbackPayload(
                TelegramCallbackAction.fromCode(parts[1]),
                decodeUuid(parts[2]),
                index,
                decodeUuid(parts[4]),
                legacy ? "" : parts[5],
                expiresAt
        );
    }

    private static String encodeUuid(UUID uuid) {
        if (uuid == null) {
            return "";
        }
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    private static UUID decodeUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
