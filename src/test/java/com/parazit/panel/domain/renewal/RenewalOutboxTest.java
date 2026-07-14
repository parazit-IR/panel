package com.parazit.panel.domain.renewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RenewalOutboxTest {

    @Test
    void createsPendingApplyRequestedOutboxWithoutPayloadInToString() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID provisionId = UUID.randomUUID();

        RenewalOutbox outbox = RenewalOutbox.create(
                orderId,
                paymentId,
                subscriptionId,
                provisionId,
                "{\"serviceUsername\":\"safe-user\"}",
                Instant.parse("2026-07-14T00:00:00Z")
        );

        assertThat(outbox.getRenewalOrderId()).isEqualTo(orderId);
        assertThat(outbox.getPaymentId()).isEqualTo(paymentId);
        assertThat(outbox.getTargetSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(outbox.getTargetProvisionId()).isEqualTo(provisionId);
        assertThat(outbox.getEventType()).isEqualTo(RenewalOutbox.APPLY_REQUESTED_EVENT_TYPE);
        assertThat(outbox.getPayloadVersion()).isEqualTo(RenewalOutbox.PAYLOAD_VERSION_V1);
        assertThat(outbox.getStatus()).isEqualTo(RenewalOutboxStatus.PENDING);
        assertThat(outbox.toString()).doesNotContain("safe-user");
    }

    @Test
    void rejectsBlankPayload() {
        assertThatThrownBy(() -> RenewalOutbox.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                " ",
                Instant.parse("2026-07-14T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
