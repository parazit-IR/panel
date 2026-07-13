package com.parazit.panel.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256SubscriptionTokenHasherTest {

    private final Sha256SubscriptionTokenHasher hasher = new Sha256SubscriptionTokenHasher();

    @Test
    void hashesTokenDeterministicallyAndMatchesSafely() {
        String token = "sub_abcdefghijklmnopqrstuvwxyzABCDEF0123456789_-";

        String hash = hasher.hash(token);

        assertThat(hash).matches("^[a-f0-9]{64}$");
        assertThat(hasher.hash(token)).isEqualTo(hash);
        assertThat(hasher.matches(token, hash)).isTrue();
        assertThat(hasher.matches("sub_abcdefghijklmnopqrstuvwxyzABCDEF0123456789_A", hash)).isFalse();
    }
}
