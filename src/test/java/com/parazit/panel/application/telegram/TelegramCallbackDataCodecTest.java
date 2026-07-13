package com.parazit.panel.application.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramCallbackPayload;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelegramCallbackDataCodecTest {

    private final TelegramCallbackDataCodec codec = new TelegramCallbackDataCodec(
            new TelegramCallbackSigner(TelegramTestProperties.properties()),
            TelegramTestProperties.properties()
    );

    @Test
    void encodesAndDecodesSignedPayloadWithoutSecrets() {
        Instant now = Instant.parse("2026-07-13T10:00:00Z");
        UUID subscriptionId = UUID.fromString("11111111-1111-4111-8111-111111111111");
        TelegramCallbackPayload payload = new TelegramCallbackPayload(
                TelegramCallbackAction.SHOW_CONFIG,
                subscriptionId,
                1,
                null,
                now.plusSeconds(60)
        );

        String encoded = codec.encode(payload, 42L);

        assertThat(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(64);
        assertThat(encoded).doesNotContain(subscriptionId.toString());
        assertThat(codec.decode(encoded, 42L, now)).isEqualTo(payload);
    }

    @Test
    void rejectsTamperingWrongActorAndExpiry() {
        Instant now = Instant.parse("2026-07-13T10:00:00Z");
        TelegramCallbackPayload payload = new TelegramCallbackPayload(
                TelegramCallbackAction.MY_SUBSCRIPTIONS,
                null,
                null,
                null,
                now.plusSeconds(60)
        );
        String encoded = codec.encode(payload, 42L);

        assertThatThrownBy(() -> codec.decode(encoded.replace('S', 'V'), 42L, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(encoded, 43L, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(encoded, 42L, now.plusSeconds(120)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
