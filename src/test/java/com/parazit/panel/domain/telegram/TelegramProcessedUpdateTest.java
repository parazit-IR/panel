package com.parazit.panel.domain.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TelegramProcessedUpdateTest {

    @Test
    void claimCompleteAndFailTransitionsAreDeterministic() {
        Instant now = Instant.parse("2026-07-13T10:00:00Z");
        TelegramProcessedUpdate update = TelegramProcessedUpdate.receive(100L, now);

        update.claim(now.plusSeconds(1), "start", 3);
        assertThat(update.getStatus()).isEqualTo(TelegramUpdateProcessingStatus.PROCESSING);
        assertThat(update.getAttemptCount()).isEqualTo(1);

        update.markProcessed(now.plusSeconds(2), "abc");
        assertThat(update.getStatus()).isEqualTo(TelegramUpdateProcessingStatus.PROCESSED);
        assertThat(update.canClaim(3)).isFalse();
    }

    @Test
    void maxAttemptsTurnsFailureDead() {
        Instant now = Instant.parse("2026-07-13T10:00:00Z");
        TelegramProcessedUpdate update = TelegramProcessedUpdate.receive(100L, now);

        update.claim(now.plusSeconds(1), "start", 1);
        update.markFailed(now.plusSeconds(2), "TEMP", "temporary", 1);

        assertThat(update.getStatus()).isEqualTo(TelegramUpdateProcessingStatus.DEAD);
    }

    @Test
    void rejectsNegativeUpdateId() {
        assertThatThrownBy(() -> TelegramProcessedUpdate.receive(-1L, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
