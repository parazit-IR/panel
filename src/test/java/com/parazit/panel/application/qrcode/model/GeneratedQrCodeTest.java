package com.parazit.panel.application.qrcode.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeneratedQrCodeTest {

    @Test
    void defensivelyCopiesBytesAndKeepsToStringSafe() {
        byte[] bytes = new byte[] {1, 2, 3};
        GeneratedQrCode code = new GeneratedQrCode(bytes, QrImageFormat.PNG, "image/png", 128, 128, "hash");
        bytes[0] = 9;

        assertThat(code.bytes()).containsExactly(1, 2, 3);
        byte[] returned = code.bytes();
        returned[1] = 9;
        assertThat(code.bytes()).containsExactly(1, 2, 3);
        assertThat(code.toString()).doesNotContain("hash");
    }
}

