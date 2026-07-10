package com.parazit.panel.infrastructure.time;

import com.parazit.panel.application.port.out.SystemClockPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class SystemClockAdapter implements SystemClockPort {

    private final Clock clock;

    public SystemClockAdapter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}
