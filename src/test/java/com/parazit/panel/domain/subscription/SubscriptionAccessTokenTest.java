package com.parazit.panel.domain.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubscriptionAccessTokenTest {

    @Test
    void validatesOpaqueUrlSafeTokenAndRedactsToString() {
        SubscriptionAccessToken token = SubscriptionAccessToken.parse("sub_abcdefghijklmnopqrstuvwxyzABCDEF0123456789_-");

        assertThat(token.rawToken()).startsWith("sub_");
        assertThat(token.safePrefix(12)).hasSize(12);
        assertThat(token.toString()).doesNotContain(token.rawToken()).contains("redacted");
    }

    @Test
    void rejectsMalformedTokens() {
        assertThatThrownBy(() -> SubscriptionAccessToken.parse("bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SubscriptionAccessToken.parse("sub_contains/slash"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
