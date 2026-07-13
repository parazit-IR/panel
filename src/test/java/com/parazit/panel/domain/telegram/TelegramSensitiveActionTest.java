package com.parazit.panel.domain.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelegramSensitiveActionTest {

    @Test
    void pendingRotationCompletesOnceAndStoresNoSecret() {
        Instant now = Instant.parse("2026-07-13T10:00:00Z");
        TelegramSensitiveAction action = TelegramSensitiveAction.pendingRotation(
                42L,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                now.plusSeconds(60)
        );

        action.complete(now.plusSeconds(1), "fingerprint");

        assertThat(action.getStatus()).isEqualTo(TelegramSensitiveActionStatus.COMPLETED);
        assertThat(action.toString()).doesNotContain("fingerprint");
    }
}
