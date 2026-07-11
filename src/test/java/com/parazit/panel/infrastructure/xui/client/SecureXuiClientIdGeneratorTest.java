package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecureXuiClientIdGeneratorTest {

    @Test
    void generatesCanonicalUuidValues() {
        SecureXuiClientIdGenerator generator = new SecureXuiClientIdGenerator();

        String first = generator.generateClientId();
        String second = generator.generateClientId();

        assertThat(UUID.fromString(first).toString()).isEqualTo(first);
        assertThat(second).isNotEqualTo(first);
    }
}
