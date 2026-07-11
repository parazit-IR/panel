package com.parazit.panel.application.xui.client.model;

import java.time.Instant;

public record XuiClientTrafficSnapshot(
        long uploadBytes,
        long downloadBytes,
        long totalConsumedBytes,
        long configuredLimitBytes,
        Long remainingBytes,
        Instant observedAt
) {
}
