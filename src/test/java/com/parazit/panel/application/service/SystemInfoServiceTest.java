package com.parazit.panel.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.out.SystemClockPort;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SystemInfoServiceTest {

    @Test
    void currentTimeReturnsInstantFromClockPort() {
        Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
        SystemClockPort clockPort = () -> fixedInstant;
        SystemInfoService service = new SystemInfoService(clockPort);

        assertThat(service.currentTime()).isEqualTo(fixedInstant);
    }
}
