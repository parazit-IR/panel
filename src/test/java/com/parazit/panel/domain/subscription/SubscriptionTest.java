package com.parazit.panel.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    void activeSubscriptionStoresOwnershipAndTokenMetadataWithoutRawToken() {
        Subscription subscription = subscription();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getTokenVersion()).isEqualTo(1);
        assertThat(subscription.getAccessCount()).isZero();
        assertThat(subscription.toString()).doesNotContain(hash()).doesNotContain("sub_");
    }

    @Test
    void lifecycleTransitionsAreDeterministic() {
        Subscription subscription = subscription();

        subscription.suspend();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        subscription.resume(NOW);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        subscription.rotateToken(hash('b'), "sub_prefix_b");
        assertThat(subscription.getTokenVersion()).isEqualTo(2);
        subscription.recordAccess(NOW.plusSeconds(1));
        assertThat(subscription.getAccessCount()).isEqualTo(1);
        subscription.revoke(NOW.plusSeconds(2), "user requested");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.REVOKED);
        assertThat(subscription.getRevokedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThatThrownBy(() -> subscription.rotateToken(hash('c'), "sub_prefix_c"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expiredAndInvalidAreTerminalForAccess() {
        Subscription expired = subscription();
        expired.expire(NOW.plusSeconds(10));
        assertThat(expired.isAccessibleAt(NOW.plusSeconds(10))).isFalse();

        Subscription invalid = subscription();
        invalid.markInvalid();
        assertThat(invalid.isTerminal()).isTrue();
    }

    private static Subscription subscription() {
        return Subscription.activate(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                7,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                hash(),
                "sub_prefix",
                NOW,
                NOW.plusSeconds(3600),
                "Plan",
                "v1"
        );
    }

    private static String hash() {
        return hash('a');
    }

    private static String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
