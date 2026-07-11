package com.parazit.panel.infrastructure.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.infrastructure.xui.dto.inbound.XuiInboundRemoteDto;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class XuiInboundMapperTest {

    private final XuiInboundMapper mapper = new XuiInboundMapper(new XuiInboundPayloadParser(new ObjectMapper()));

    @Test
    void mapsRemoteDtoToSnapshot() {
        XuiInboundSnapshot snapshot = mapper.toSnapshot(remote(7L, 443, " vless ", " Reality Main "));

        assertThat(snapshot.id()).isEqualTo(7);
        assertThat(snapshot.remark()).isEqualTo("Reality Main");
        assertThat(snapshot.protocol()).isEqualTo("VLESS");
        assertThat(snapshot.port()).isEqualTo(443);
        assertThat(snapshot.enabled()).isTrue();
        assertThat(snapshot.listenAddress()).isNull();
        assertThat(snapshot.totalTrafficLimitBytes()).isEqualTo(1000);
        assertThat(snapshot.uploadBytes()).isEqualTo(10);
        assertThat(snapshot.downloadBytes()).isEqualTo(20);
        assertThat(snapshot.expiryTime()).isEqualTo(Instant.ofEpochMilli(1893456000000L));
        assertThat(snapshot.clients()).hasSize(1);
        assertThat(snapshot.streamNetwork()).isEqualTo("tcp");
        assertThat(snapshot.securityType()).isEqualTo("REALITY");
        assertThat(snapshot.serverName()).isEqualTo("vpn.example.test");
        assertThat(snapshot.publicKey()).isEqualTo("PUBLIC_KEY");
        assertThat(snapshot.shortId()).isEqualTo("abcd");
        assertThat(snapshot.toString()).doesNotContain("PRIVATE_KEY");
    }

    @Test
    void rejectsMalformedRequiredValues() {
        assertThatThrownBy(() -> mapper.toSnapshot(remote(null, 443, "vless", "main")))
                .isInstanceOf(XuiInvalidResponseException.class);
        assertThatThrownBy(() -> mapper.toSnapshot(remote(7L, 0, "vless", "main")))
                .isInstanceOf(XuiInvalidResponseException.class);
        assertThatThrownBy(() -> mapper.toSnapshot(remote(7L, 443, " ", "main")))
                .isInstanceOf(XuiInvalidResponseException.class);
    }

    private static XuiInboundRemoteDto remote(Long id, Integer port, String protocol, String remark) {
        return new XuiInboundRemoteDto(
                id,
                10L,
                20L,
                1000L,
                remark,
                true,
                1893456000000L,
                "",
                port,
                protocol,
                "{\"clients\":[{\"id\":\"client-1\"}]}",
                "{\"network\":\"tcp\",\"security\":\"reality\",\"realitySettings\":{\"serverNames\":[\"vpn.example.test\"],\"shortIds\":[\"abcd\"],\"settings\":{\"publicKey\":\"PUBLIC_KEY\",\"privateKey\":\"PRIVATE_KEY\"}}}"
        );
    }
}
