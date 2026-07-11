package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.infrastructure.xui.dto.client.XuiCreateClientRemoteRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class XuiCreateClientPayloadBuilderTest {

    @Test
    void buildsVerifiedAddClientPayload() throws Exception {
        XuiCreateClientRemoteRequest payload = new XuiCreateClientPayloadBuilder().build(new CreateXuiClientRequest(
                7,
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                "sub123",
                true,
                1024,
                Instant.ofEpochMilli(1893456000000L),
                2,
                "xtls-rprx-vision"
        ));

        String json = new ObjectMapper().writeValueAsString(payload);

        assertThat(json).contains("\"id\":7");
        assertThat(json).contains("\"settings\":{\"clients\":[");
        assertThat(json).contains("\"id\":\"11111111-1111-1111-1111-111111111111\"");
        assertThat(json).contains("\"email\":\"vpn_abc_def\"");
        assertThat(json).contains("\"totalGB\":1024");
        assertThat(json).contains("\"expiryTime\":1893456000000");
        assertThat(json).contains("\"limitIp\":2");
        assertThat(json).contains("\"subId\":\"sub123\"");
        assertThat(json).doesNotContain("\\\"clients\\\"");
    }
}
