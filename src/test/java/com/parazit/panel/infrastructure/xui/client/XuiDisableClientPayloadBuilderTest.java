package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.xui.client.model.DisableXuiClientRequest;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.dto.client.XuiUpdateClientRemoteRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class XuiDisableClientPayloadBuilderTest {

    @Test
    void preservesRemoteClientFieldsAndOnlyDisablesClient() {
        XuiDisableClientPayloadBuilder builder = new XuiDisableClientPayloadBuilder();
        XuiClientSnapshot snapshot = new XuiClientSnapshot(
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                true,
                1024,
                10,
                20,
                Instant.ofEpochMilli(1893456000000L),
                2,
                "sub123",
                "xtls-rprx-vision"
        );

        XuiUpdateClientRemoteRequest payload = builder.build(
                new DisableXuiClientRequest(7, snapshot.clientId(), snapshot.email()),
                snapshot
        );

        assertThat(payload.id()).isEqualTo(7);
        assertThat(payload.settings().clients()).hasSize(1);
        assertThat(payload.settings().clients().getFirst().id()).isEqualTo(snapshot.clientId());
        assertThat(payload.settings().clients().getFirst().email()).isEqualTo(snapshot.email());
        assertThat(payload.settings().clients().getFirst().flow()).isEqualTo(snapshot.flow());
        assertThat(payload.settings().clients().getFirst().totalGB()).isEqualTo(1024);
        assertThat(payload.settings().clients().getFirst().expiryTime()).isEqualTo(1893456000000L);
        assertThat(payload.settings().clients().getFirst().limitIp()).isEqualTo(2);
        assertThat(payload.settings().clients().getFirst().subId()).isEqualTo("sub123");
        assertThat(payload.settings().clients().getFirst().enable()).isFalse();
    }
}
