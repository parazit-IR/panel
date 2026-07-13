package com.parazit.panel.application.subscription.render;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.subscription.model.RenderedSubscription;
import com.parazit.panel.application.subscription.model.SubscriptionConfigEntry;
import com.parazit.panel.application.subscription.model.SubscriptionContent;
import com.parazit.panel.application.subscription.model.VlessSubscriptionConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultSubscriptionRendererTest {

    private final DefaultSubscriptionRenderer renderer = new DefaultSubscriptionRenderer(new VlessUriBuilder());

    @Test
    void rendersPlainAndBase64DeterministicallyWithSafeHeaders() {
        SubscriptionContent content = new SubscriptionContent(
                "Panel VPN",
                List.of(new SubscriptionConfigEntry(config("One")), new SubscriptionConfigEntry(config("Two"))),
                Instant.ofEpochSecond(1_800_000_000),
                10L,
                20L,
                100L,
                70L,
                "https://example.com/support",
                "24"
        );

        RenderedSubscription plain = renderer.renderPlain(content);
        RenderedSubscription base64 = renderer.renderBase64(content);

        String plainText = new String(plain.body(), StandardCharsets.UTF_8);
        assertThat(plainText.lines()).hasSize(2);
        assertThat(new String(Base64.getDecoder().decode(base64.body()), StandardCharsets.UTF_8)).isEqualTo(plainText);
        assertThat(plain.headers().values())
                .containsEntry("subscription-userinfo", "upload=10; download=20; total=100; expire=1800000000")
                .containsEntry("profile-update-interval", "24");

        byte[] first = base64.body();
        first[0] = 'x';
        assertThat(base64.body()[0]).isNotEqualTo((byte) 'x');
    }

    private static VlessSubscriptionConfig config(String remark) {
        return new VlessSubscriptionConfig(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "vpn.example.test",
                443,
                "none",
                "reality",
                "sni.example.test",
                "PUBLIC_KEY_TEST",
                "abcd1234",
                "chrome",
                null,
                "tcp",
                null,
                null,
                null,
                null,
                remark
        );
    }
}
