package com.parazit.panel.application.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class XuiInboundEligibilityPolicyTest {

    private final XuiInboundEligibilityPolicy policy = new XuiInboundEligibilityPolicy();

    @Test
    void acceptsEnabledVlessRealityInbound() {
        assertThat(policy.isEligible(inbound(true, "vless", "reality", 443, "pk", "sid", "server"))).isTrue();
    }

    @Test
    void rejectsDisabledUnsupportedProtocolAndMissingRealityFields() {
        assertThat(policy.isEligible(inbound(false, "vless", "reality", 443, "pk", "sid", "server"))).isFalse();
        assertThat(policy.isEligible(inbound(true, "vmess", "reality", 443, "pk", "sid", "server"))).isFalse();
        assertThat(policy.isEligible(inbound(true, "vless", "tls", 443, "pk", "sid", "server"))).isFalse();
        assertThat(policy.isEligible(inbound(true, "vless", "reality", 0, "pk", "sid", "server"))).isFalse();
        assertThat(policy.isEligible(inbound(true, "vless", "reality", 443, null, "sid", "server"))).isFalse();
        assertThat(policy.isEligible(inbound(true, "vless", "reality", 443, "pk", null, "server"))).isFalse();
        assertThat(policy.isEligible(inbound(true, "vless", "reality", 443, "pk", "sid", null))).isFalse();
    }

    @Test
    void doesNotMutateInbound() {
        XuiInboundSnapshot inbound = inbound(true, "vless", "reality", 443, "pk", "sid", "server");

        policy.isEligible(inbound);

        assertThat(inbound.protocol()).isEqualTo("vless");
        assertThat(inbound.securityType()).isEqualTo("reality");
    }

    static XuiInboundSnapshot inbound(
            boolean enabled,
            String protocol,
            String securityType,
            int port,
            String publicKey,
            String shortId,
            String serverName
    ) {
        return new XuiInboundSnapshot(
                7,
                "main",
                protocol,
                port,
                enabled,
                null,
                0,
                0,
                0,
                null,
                List.of(),
                "tcp",
                securityType,
                serverName,
                publicKey,
                shortId
        );
    }
}
