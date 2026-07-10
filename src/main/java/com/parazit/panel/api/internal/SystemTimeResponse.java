package com.parazit.panel.api.internal;

import java.time.Instant;

public record SystemTimeResponse(
        Instant currentTime
) {
}
