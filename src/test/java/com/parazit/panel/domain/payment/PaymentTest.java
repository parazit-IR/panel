package com.parazit.panel.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-07-12T12:30:00Z");
    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    @Test
    void createStartsCreatedAndNormalizesCurrency() {
        Payment payment = payment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.ZARINPAL);
        assertThat(payment.getBaseAmount()).isEqualTo(100_000L);
        assertThat(payment.getPayableAmount()).isEqualTo(100_000L);
        assertThat(payment.getCurrency()).isEqualTo("IRT");
        assertThat(payment.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void validatesAmountsAndRequiredFields() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> Payment.create(orderId, userId, PaymentMethod.ZARINPAL, -1, 0, "IRT", EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseAmount");
        assertThatThrownBy(() -> Payment.create(orderId, userId, PaymentMethod.ZARINPAL, 10, 9, "IRT", EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payableAmount");
        assertThatThrownBy(() -> Payment.create(orderId, userId, null, 10, 10, "IRT", EXPIRES_AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void supportsWaitingProcessingAndApproval() {
        Payment payment = payment();

        payment.markWaitingForPayment();
        payment.markProcessing();
        payment.markApproved(NOW, "txn-1", "authority-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        assertThat(payment.getApprovedAt()).isEqualTo(NOW);
        assertThat(payment.getGatewayTransactionId()).isEqualTo("txn-1");
        assertThat(payment.getGatewayAuthority()).isEqualTo("authority-1");
    }

    @Test
    void supportsRejectedExpiredCancelledAndFailedTransitions() {
        Payment rejected = payment();
        rejected.markWaitingForPayment();
        rejected.markRejected(NOW, "bad receipt");

        Payment expired = payment();
        expired.markWaitingForPayment();
        expired.markExpired(NOW);

        Payment cancelled = payment();
        cancelled.markCancelled(NOW);

        Payment failed = payment();
        failed.markProcessing();
        failed.markFailed(NOW, "provider failure");

        assertThat(rejected.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(rejected.getRejectedAt()).isEqualTo(NOW);
        assertThat(expired.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(cancelled.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void rejectsInvalidTransitions() {
        Payment payment = payment();

        assertThatThrownBy(() -> payment.markApproved(NOW, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CREATED");

        payment.markCancelled(NOW);

        assertThatThrownBy(payment::markWaitingForPayment)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    private Payment payment() {
        return Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentMethod.ZARINPAL,
                100_000L,
                100_000L,
                " irt ",
                EXPIRES_AT
        );
    }
}
