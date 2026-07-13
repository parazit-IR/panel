package com.parazit.panel.application.telegram.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

class TelegramSupportUrlFactoryTest {

    private final TelegramSupportUrlFactory factory = new TelegramSupportUrlFactory();

    @Test
    void normalizesUsernameAndBuildsTrustedTelegramUrl() {
        assertThat(factory.create("@Support_User1")).isEqualTo(URI.create("https://t.me/Support_User1"));
    }

    @Test
    void rejectsInvalidUsernameInsteadOfArbitraryUrl() {
        assertThatThrownBy(() -> factory.create("https://example.com/support"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.create("ab"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
