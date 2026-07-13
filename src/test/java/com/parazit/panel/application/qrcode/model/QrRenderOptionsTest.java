package com.parazit.panel.application.qrcode.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QrRenderOptionsTest {

    @Test
    void appliesSafeDefaults() {
        QrRenderOptions options = new QrRenderOptions(512, 512, 4, null, null, false);

        assertThat(options.errorCorrection()).isEqualTo(QrErrorCorrection.MEDIUM);
        assertThat(options.format()).isEqualTo(QrImageFormat.PNG);
    }

    @Test
    void rejectsInvalidDimensionsAndMargin() {
        assertThatThrownBy(() -> new QrRenderOptions(512, 256, 4, QrErrorCorrection.MEDIUM, QrImageFormat.PNG, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QrRenderOptions(512, 512, -1, QrErrorCorrection.MEDIUM, QrImageFormat.PNG, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

