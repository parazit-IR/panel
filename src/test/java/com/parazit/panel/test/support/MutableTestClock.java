package com.parazit.panel.test.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class MutableTestClock extends Clock {

    private volatile Instant instant;

    public MutableTestClock(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant must not be null");
    }

    public void setInstant(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant must not be null");
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
