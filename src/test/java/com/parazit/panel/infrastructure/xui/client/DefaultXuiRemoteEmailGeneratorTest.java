package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultXuiRemoteEmailGeneratorTest {

    @Test
    void generatesDeterministicBoundedLowercaseLabelWithoutFullUuid() {
        DefaultXuiRemoteEmailGenerator generator = new DefaultXuiRemoteEmailGenerator();
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID provisionId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        String email = generator.generate(userId, provisionId);

        assertThat(email).isEqualTo(generator.generate(userId, provisionId));
        assertThat(email).matches("vpn_[a-f0-9]{12}_[a-f0-9]{12}");
        assertThat(email).doesNotContain(userId.toString());
        assertThat(email).hasSizeLessThanOrEqualTo(128);
    }
}
