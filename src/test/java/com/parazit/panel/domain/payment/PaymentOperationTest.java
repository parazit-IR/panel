package com.parazit.panel.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentOperationTest {

    @Test
    void recordsSanitizedHistoryEntry() {
        UUID paymentId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-12T12:00:00Z");

        PaymentOperation operation = PaymentOperation.record(
                paymentId,
                PaymentOperationType.CREATED,
                occurredAt,
                " created "
        );

        assertThat(operation.getPaymentId()).isEqualTo(paymentId);
        assertThat(operation.getOperationType()).isEqualTo(PaymentOperationType.CREATED);
        assertThat(operation.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(operation.getMessage()).isEqualTo("created");
    }

    @Test
    void validatesRequiredFields() {
        assertThatThrownBy(() -> PaymentOperation.record(null, PaymentOperationType.CREATED, Instant.now(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PaymentOperation.record(UUID.randomUUID(), null, Instant.now(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PaymentOperation.record(UUID.randomUUID(), PaymentOperationType.CREATED, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
