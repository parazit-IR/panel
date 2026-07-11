package com.parazit.panel.test.support;

import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MutableClockTestConfiguration {

    public static final Instant DEFAULT_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    @Bean
    @Primary
    Clock mutableTestClock() {
        return new MutableTestClock(DEFAULT_INSTANT);
    }
}
