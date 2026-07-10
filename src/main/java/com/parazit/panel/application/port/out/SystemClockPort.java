package com.parazit.panel.application.port.out;

import java.time.Instant;

public interface SystemClockPort {

    Instant now();
}
