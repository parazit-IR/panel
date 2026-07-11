package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.xui.client.model.UpdateXuiClientRequest;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.dto.client.XuiUpdateClientRemoteRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class XuiUpdateClientPayloadBuilderTest {

    @Test
    void preservesUnchangedFieldsAndPatchesOnlyRequestedValues() {
        XuiClientSnapshot current = new XuiClientSnapshot(
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                false,
                1024,
                10,
                20,
                Instant.ofEpochMilli(1893456000000L),
                2,
                "sub123",
                "xtls-rprx-vision",
                "tg",
                "safe comment",
                1
        );
        UpdateXuiClientRequest request = new UpdateXuiClientRequest(
                7,
                current.clientId(),
                current.email(),
                true,
                Instant.ofEpochMilli(1896048000000L),
                null,
                null,
                null
        );

        XuiUpdateClientRemoteRequest payload = new XuiUpdateClientPayloadBuilder().build(request, current);

        assertThat(payload.id()).isEqualTo(7);
        assertThat(payload.settings().clients()).hasSize(1);
        var client = payload.settings().clients().getFirst();
        assertThat(client.id()).isEqualTo(current.clientId());
        assertThat(client.email()).isEqualTo(current.email());
        assertThat(client.enable()).isTrue();
        assertThat(client.expiryTime()).isEqualTo(1896048000000L);
        assertThat(client.totalGB()).isEqualTo(1024);
        assertThat(client.limitIp()).isEqualTo(2);
        assertThat(client.subId()).isEqualTo("sub123");
        assertThat(client.flow()).isEqualTo("xtls-rprx-vision");
        assertThat(client.comment()).isEqualTo("safe comment");
        assertThat(client.reset()).isEqualTo(1);
    }
}
