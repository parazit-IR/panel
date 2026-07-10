package com.parazit.panel.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class FixedClockTestConfiguration {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
