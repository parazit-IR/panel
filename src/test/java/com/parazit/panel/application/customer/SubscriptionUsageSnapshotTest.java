package com.parazit.panel.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.customer.result.SubscriptionUsageSnapshot;
import com.parazit.panel.application.customer.result.UsageFreshness;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SubscriptionUsageSnapshotTest {

    @Test
    void clampsRemainingTrafficToZero() {
        SubscriptionUsageSnapshot snapshot = SubscriptionUsageSnapshot.of(100L, 120L, Instant.EPOCH, UsageFreshness.FRESH);

        assertThat(snapshot.remainingBytes()).hasValue(0L);
    }

    @Test
    void doesNotFabricateUnavailableUsage() {
        SubscriptionUsageSnapshot snapshot = SubscriptionUsageSnapshot.unavailable();

        assertThat(snapshot.totalBytes()).isEmpty();
        assertThat(snapshot.usedBytes()).isEmpty();
        assertThat(snapshot.freshness()).isEqualTo(UsageFreshness.UNAVAILABLE);
    }

    @Test
    void rejectsNegativeUsage() {
        assertThatThrownBy(() -> SubscriptionUsageSnapshot.of(100L, -1L, Instant.EPOCH, UsageFreshness.FRESH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
