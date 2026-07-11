package com.parazit.panel.infrastructure.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class XuiInboundPayloadParserTest {

    private final XuiInboundPayloadParser parser = new XuiInboundPayloadParser(new ObjectMapper());

    @Test
    void parsesVlessClientsAndRealityMetadata() {
        String settings = """
                {"clients":[{"id":"client-1","email":"client@example.test","enable":true,"totalGB":1000,
                "up":10,"down":20,"expiryTime":1893456000000,"limitIp":2,"subId":"sub-1"}]}
                """;
        String streamSettings = """
                {"network":"tcp","security":"reality","realitySettings":{"serverNames":["vpn.example.test"],
                "shortIds":["abcd"],"settings":{"publicKey":"PUBLIC_KEY","privateKey":"PRIVATE_KEY"}}}
                """;

        XuiInboundPayload payload = parser.parse(settings, streamSettings);

        assertThat(payload.clients()).hasSize(1);
        assertThat(payload.clients().getFirst().clientId()).isEqualTo("client-1");
        assertThat(payload.clients().getFirst().email()).isEqualTo("client@example.test");
        assertThat(payload.clients().getFirst().enabled()).isTrue();
        assertThat(payload.clients().getFirst().totalTrafficLimitBytes()).isEqualTo(1000);
        assertThat(payload.clients().getFirst().uploadBytes()).isEqualTo(10);
        assertThat(payload.clients().getFirst().downloadBytes()).isEqualTo(20);
        assertThat(payload.clients().getFirst().expiryTime()).isEqualTo(Instant.ofEpochMilli(1893456000000L));
        assertThat(payload.clients().getFirst().ipLimit()).isEqualTo(2);
        assertThat(payload.clients().getFirst().subscriptionId()).isEqualTo("sub-1");
        assertThat(payload.streamNetwork()).isEqualTo("tcp");
        assertThat(payload.securityType()).isEqualTo("REALITY");
        assertThat(payload.serverName()).isEqualTo("vpn.example.test");
        assertThat(payload.publicKey()).isEqualTo("PUBLIC_KEY");
        assertThat(payload.shortId()).isEqualTo("abcd");
        assertThat(payload.toString()).doesNotContain("PRIVATE_KEY");
    }

    @Test
    void handlesBlankOptionalPayloadsSafely() {
        XuiInboundPayload payload = parser.parse("", null);

        assertThat(payload.clients()).isEmpty();
        assertThat(payload.streamNetwork()).isNull();
        assertThat(payload.securityType()).isNull();
        assertThat(payload.publicKey()).isNull();
    }

    @Test
    void toleratesTlsStreamSettingsWithoutRealityMetadata() {
        XuiInboundPayload payload = parser.parse("{\"clients\":[]}", "{\"network\":\"tcp\",\"security\":\"tls\"}");

        assertThat(payload.clients()).isEmpty();
        assertThat(payload.streamNetwork()).isEqualTo("tcp");
        assertThat(payload.securityType()).isEqualTo("TLS");
        assertThat(payload.publicKey()).isNull();
        assertThat(payload.shortId()).isNull();
    }

    @Test
    void convertsZeroAndNegativeExpiryToNullAndPositiveMillisToInstant() {
        assertThat(parser.toInstant(0L)).isNull();
        assertThat(parser.toInstant(-1L)).isNull();
        assertThat(parser.toInstant(1L)).isEqualTo(Instant.ofEpochMilli(1L));
    }

    @Test
    void rejectsMalformedSettingsJsonAndNegativeTraffic() {
        assertThatThrownBy(() -> parser.parse("{\"clients\":[", "{}"))
                .isInstanceOf(XuiInvalidResponseException.class)
                .hasMessageContaining("settings is malformed");

        assertThatThrownBy(() -> parser.parse("{\"clients\":[{\"totalGB\":-1}]}", "{}"))
                .isInstanceOf(XuiInvalidResponseException.class)
                .hasMessageContaining("traffic");
    }

    @Test
    void rejectsNonArrayClients() {
        assertThatThrownBy(() -> parser.parse("{\"clients\":{}}", "{}"))
                .isInstanceOf(XuiInvalidResponseException.class)
                .hasMessageContaining("clients must be an array");
    }
}
