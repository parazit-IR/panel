package com.parazit.panel.domain.payment.zarinpal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZarinpalPaymentAttemptTest {

    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    @Test
    void createsAndTransitionsToVerified() {
        ZarinpalPaymentAttempt attempt = attempt();

        attempt.markRequesting(NOW);
        attempt.markRedirectReady(" A000000000000000000000000000123456 ", NOW.plusSeconds(1), "100");
        attempt.markCallbackReceived(NOW.plusSeconds(2));
        attempt.markVerifying(NOW.plusSeconds(3));
        attempt.markVerified("987654321", "100", "hash", "502229******5995", NOW.plusSeconds(4));

        assertThat(attempt.getStatus()).isEqualTo(ZarinpalAttemptStatus.VERIFIED);
        assertThat(attempt.getAuthority()).isEqualTo("A000000000000000000000000000123456");
        assertThat(attempt.getReferenceId()).isEqualTo("987654321");
        assertThat(attempt.getCardPanMasked()).isEqualTo("502229******5995");
    }

    @Test
    void supportsCancellationFailureAndUnknown() {
        ZarinpalPaymentAttempt cancelled = attempt();
        cancelled.markRequesting(NOW);
        cancelled.markRedirectReady("A000000000000000000000000000123456", NOW, "100");
        cancelled.markCancelled(NOW);

        ZarinpalPaymentAttempt failed = attempt();
        failed.markRequesting(NOW);
        failed.markFailed("-9", "failure", NOW);

        ZarinpalPaymentAttempt unknown = attempt();
        unknown.markRequesting(NOW);
        unknown.markUnknown("TIMEOUT", "unknown", NOW);

        assertThat(cancelled.getStatus()).isEqualTo(ZarinpalAttemptStatus.CANCELLED);
        assertThat(failed.getStatus()).isEqualTo(ZarinpalAttemptStatus.FAILED);
        assertThat(unknown.getStatus()).isEqualTo(ZarinpalAttemptStatus.UNKNOWN);
    }

    @Test
    void rejectsInvalidValuesAndTransitions() {
        assertThatThrownBy(() -> ZarinpalPaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> attempt().markRedirectReady(null, NOW, "100"))
                .isInstanceOf(IllegalStateException.class);
        ZarinpalPaymentAttempt attempt = attempt();
        attempt.markRequesting(NOW);
        assertThatThrownBy(() -> attempt.markVerified("ref", "100", null, null, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    private ZarinpalPaymentAttempt attempt() {
        return ZarinpalPaymentAttempt.create(UUID.randomUUID(), UUID.randomUUID(), 100_000L);
    }
}
