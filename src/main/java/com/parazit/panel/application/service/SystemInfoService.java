package com.parazit.panel.application.service;

import com.parazit.panel.application.port.out.SystemClockPort;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SystemInfoService {

    private final SystemClockPort systemClockPort;

    public SystemInfoService(SystemClockPort systemClockPort) {
        this.systemClockPort = Objects.requireNonNull(systemClockPort, "systemClockPort must not be null");
    }

    public Instant currentTime() {
        return systemClockPort.now();
    }
}
