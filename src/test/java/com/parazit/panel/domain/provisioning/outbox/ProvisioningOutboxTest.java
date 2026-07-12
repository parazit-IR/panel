package com.parazit.panel.domain.provisioning.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProvisioningOutboxTest {

    @Test
    void processingFailureRetryAndProcessedStatesAreControlled() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        ProvisioningOutbox outbox = outbox(now);

        assertThat(outbox.isAvailable(now)).isTrue();
        outbox.markProcessing(now);
        assertThat(outbox.getStatus()).isEqualTo(ProvisioningOutboxStatus.PROCESSING);
        assertThat(outbox.isProcessingStale(now.plus(Duration.ofMinutes(6)), Duration.ofMinutes(5))).isTrue();

        outbox.markFailed("XUI_TIMEOUT", "temporary", now.plusSeconds(1), now.plusSeconds(11));
        assertThat(outbox.getStatus()).isEqualTo(ProvisioningOutboxStatus.FAILED);
        assertThat(outbox.getAttemptCount()).isEqualTo(1);

        outbox.retryNow(now.plusSeconds(12));
        outbox.markProcessing(now.plusSeconds(12));
        outbox.markProcessed(now.plusSeconds(13));

        assertThat(outbox.getStatus()).isEqualTo(ProvisioningOutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isEqualTo(now.plusSeconds(13));
    }

    @Test
    void processedOutboxCannotBeRetriedOrMarkedDead() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        ProvisioningOutbox outbox = outbox(now);
        outbox.markProcessing(now);
        outbox.markProcessed(now.plusSeconds(1));

        assertThatThrownBy(() -> outbox.retryNow(now.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> outbox.markDead("ERR", "message", now.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ProvisioningOutbox outbox(Instant availableAt) {
        return ProvisioningOutbox.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ProvisioningOutboxType.CREATE_VPN_CLIENT,
                "create-vpn-client.v1",
                "{\"orderId\":\"00000000-0000-0000-0000-000000000000\"}",
                availableAt
        );
    }
}
