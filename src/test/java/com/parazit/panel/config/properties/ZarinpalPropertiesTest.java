package com.parazit.panel.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ZarinpalPropertiesTest {

    @Test
    void defaultsSafelyWhenDisabled() {
        ZarinpalProperties properties = new ZarinpalProperties(
                false, "", null, null, null, null, null, null, null, null,
                null, null, 0, null, false, true
        );

        assertThat(properties.apiBaseUrl()).isEqualTo(URI.create("https://api.zarinpal.com"));
        assertThat(properties.requestPath()).isEqualTo("/pg/v4/payment/request.json");
        assertThat(properties.verifySsl()).isTrue();
        assertThat(properties.toString()).doesNotContain("real-merchant");
    }

    @Test
    void validatesEnabledMerchantUrlsAndTimeouts() {
        assertThatThrownBy(() -> new ZarinpalProperties(
                true, "", null, null, null, null, null, null, null, null,
                null, null, 0, null, false, true
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZarinpalProperties(
                false, "", URI.create("/relative"), null, null, null, null, null, null, null,
                Duration.ofSeconds(1), Duration.ofSeconds(1), 0, Duration.ofSeconds(1), false, true
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ZarinpalProperties(
                false, "", null, null, "relative", null, null, null, null, null,
                Duration.ofSeconds(1), Duration.ofSeconds(1), 0, Duration.ofSeconds(1), false, true
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
