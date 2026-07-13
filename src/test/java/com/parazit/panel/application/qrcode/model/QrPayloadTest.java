package com.parazit.panel.application.qrcode.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QrPayloadTest {

    @Test
    void acceptsSupportedPayloadTypesAndKeepsToStringSafe() {
        QrPayload subscription = new QrPayload(QrPayloadType.SUBSCRIPTION_URL, "https://example.test/sub/sub_secret");
        QrPayload vless = new QrPayload(QrPayloadType.VLESS_URI, "vless://11111111-1111-1111-1111-111111111111@example.test:443");

        assertThat(subscription.value()).isEqualTo("https://example.test/sub/sub_secret");
        assertThat(vless.value()).startsWith("vless://");
        assertThat(subscription.toString()).doesNotContain("sub_secret");
        assertThat(vless.toString()).doesNotContain("11111111");
    }

    @Test
    void rejectsInvalidPayloads() {
        assertThatThrownBy(() -> new QrPayload(QrPayloadType.SUBSCRIPTION_URL, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QrPayload(QrPayloadType.SUBSCRIPTION_URL, "https://example.test/other/sub_secret"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QrPayload(QrPayloadType.VLESS_URI, "https://example.test/sub/sub_secret"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QrPayload(QrPayloadType.VLESS_URI, "vless://ok\nbad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesPayloadLength() {
        QrPayload payload = new QrPayload(QrPayloadType.SUBSCRIPTION_URL, "https://example.test/sub/sub_secret");

        assertThatThrownBy(() -> payload.validateLength(10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

