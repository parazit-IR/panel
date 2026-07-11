package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SecureXuiSubscriptionIdGeneratorTest {

    @Test
    void generatesUrlSafeIdentifiersWithConfiguredLength() {
        SecureXuiSubscriptionIdGenerator generator = new SecureXuiSubscriptionIdGenerator(properties(24));

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).hasSize(24).matches("[a-z0-9]+");
        assertThat(second).hasSize(24).matches("[a-z0-9]+");
        assertThat(first).isNotEqualTo(second);
    }

    private static XuiProperties properties(int length) {
        return new XuiProperties(
                "http://localhost:2053",
                "admin",
                "secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                0,
                Duration.ZERO,
                true,
                true,
                Duration.ofMinutes(30),
                "/panel/api/inbounds/list",
                "/panel/api/inbounds/addClient",
                "xtls-rprx-vision",
                length,
                1
        );
    }
}
