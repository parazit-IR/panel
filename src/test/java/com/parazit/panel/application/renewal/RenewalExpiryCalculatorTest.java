package com.parazit.panel.application.renewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RenewalExpiryCalculatorTest {

    private final RenewalExpiryCalculator calculator = new RenewalExpiryCalculator();

    @Test
    void extendsFromCurrentExpiry() {
        Instant now = Instant.parse("2026-07-14T00:00:00Z");
        Instant currentExpiry = Instant.parse("2026-07-20T00:00:00Z");

        Instant proposed = calculator.proposedExpiry(currentExpiry, Duration.ofDays(30), RenewalExpiryPolicy.EXTEND_FROM_CURRENT_EXPIRY, now);

        assertThat(proposed).isEqualTo(Instant.parse("2026-08-19T00:00:00Z"));
    }

    @Test
    void extendsFromNow() {
        Instant now = Instant.parse("2026-07-14T00:00:00Z");
        Instant currentExpiry = Instant.parse("2026-07-20T00:00:00Z");

        Instant proposed = calculator.proposedExpiry(currentExpiry, Duration.ofDays(30), RenewalExpiryPolicy.EXTEND_FROM_NOW, now);

        assertThat(proposed).isEqualTo(Instant.parse("2026-08-13T00:00:00Z"));
    }

    @Test
    void extendsFromLaterOfNowOrExpiry() {
        Instant now = Instant.parse("2026-07-14T00:00:00Z");

        assertThat(calculator.proposedExpiry(Instant.parse("2026-07-20T00:00:00Z"), Duration.ofDays(30), RenewalExpiryPolicy.EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY, now))
                .isEqualTo(Instant.parse("2026-08-19T00:00:00Z"));
        assertThat(calculator.proposedExpiry(Instant.parse("2026-07-01T00:00:00Z"), Duration.ofDays(30), RenewalExpiryPolicy.EXTEND_FROM_LATER_OF_NOW_OR_EXPIRY, now))
                .isEqualTo(Instant.parse("2026-08-13T00:00:00Z"));
    }

    @Test
    void rejectsOverflowAndInvalidDuration() {
        Instant now = Instant.parse("2026-07-14T00:00:00Z");

        assertThatThrownBy(() -> calculator.proposedExpiry(now, Duration.ZERO, RenewalExpiryPolicy.EXTEND_FROM_NOW, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.proposedExpiry(Instant.MAX, Duration.ofDays(1), RenewalExpiryPolicy.EXTEND_FROM_CURRENT_EXPIRY, now))
                .isInstanceOf(RenewalFlowException.class);
    }
}
