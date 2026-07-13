package com.parazit.panel.application.subscription.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.subscription.UnsupportedInboundConfigurationException;
import com.parazit.panel.application.subscription.model.VlessSubscriptionConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VlessUriBuilderTest {

    private final VlessUriBuilder builder = new VlessUriBuilder();

    @Test
    void buildsExactVlessRealityTcpUriWithDeterministicQueryOrder() {
        String uri = builder.build(new VlessSubscriptionConfig(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "vpn.example.test",
                443,
                "none",
                "reality",
                "sni.example.test",
                "PUBLIC_KEY_TEST",
                "abcd1234",
                "chrome",
                "xtls-rprx-vision",
                "tcp",
                null,
                null,
                null,
                null,
                "پلن تست"
        ));

        assertThat(uri).isEqualTo("vless://11111111-1111-1111-1111-111111111111@vpn.example.test:443?encryption=none&security=reality&sni=sni.example.test&fp=chrome&pbk=PUBLIC_KEY_TEST&sid=abcd1234&type=tcp&flow=xtls-rprx-vision#%D9%BE%D9%84%D9%86%20%D8%AA%D8%B3%D8%AA");
    }

    @Test
    void formatsIpv6HostAndRejectsUnsupportedSecurity() {
        String uri = builder.build(new VlessSubscriptionConfig(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "2001:db8::1",
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
                "IPv6"
        ));
        assertThat(uri).startsWith("vless://11111111-1111-1111-1111-111111111111@[2001:db8::1]:443");

        assertThatThrownBy(() -> builder.build(new VlessSubscriptionConfig(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "vpn.example.test",
                443,
                "none",
                "tls",
                "sni",
                "pk",
                "sid",
                "chrome",
                null,
                "tcp",
                null,
                null,
                null,
                null,
                "bad"
        ))).isInstanceOf(UnsupportedInboundConfigurationException.class);
    }
}
