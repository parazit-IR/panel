package com.parazit.panel.config;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.infrastructure.time.SystemClockAdapter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SystemClockPort systemClockPort(Clock clock) {
        return new SystemClockAdapter(clock);
    }
}
