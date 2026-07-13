package com.parazit.panel.application.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.telegram.model.TelegramCallbackAction;
import com.parazit.panel.application.telegram.model.TelegramCallbackPayload;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TelegramCallbackReferenceTest {

    private final TelegramCallbackDataCodec codec = new TelegramCallbackDataCodec(
            new TelegramCallbackSigner(TelegramTestProperties.properties()),
            TelegramTestProperties.properties()
    );

    @Test
    void encodesBoundedReferenceWithoutExceedingTelegramLimit() {
        Instant now = Instant.parse("2026-07-13T10:00:00Z");
        TelegramCallbackPayload payload = new TelegramCallbackPayload(
                TelegramCallbackAction.SHOW_FAQ_ITEM,
                null,
                1,
                null,
                "faq-item-id-with-32-chars-12345",
                now.plusSeconds(60)
        );

        String encoded = codec.encode(payload, 42L);

        assertThat(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSizeLessThanOrEqualTo(64);
        assertThat(codec.decode(encoded, 42L, now)).isEqualTo(payload);
    }
}
