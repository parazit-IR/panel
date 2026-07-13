package com.parazit.panel.application.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TelegramCustomerTextFormatterTest {

    private final TelegramCustomerTextFormatter formatter = new TelegramCustomerTextFormatter();

    @Test
    void formatsTrafficAndDurationForPersian() {
        assertThat(formatter.traffic(30L * 1024L * 1024L * 1024L, "fa"))
                .isEqualTo("۳۰ گیگابایت");
        assertThat(formatter.duration(Duration.ofDays(18), "fa"))
                .isEqualTo("۱۸ روز");
    }
}
