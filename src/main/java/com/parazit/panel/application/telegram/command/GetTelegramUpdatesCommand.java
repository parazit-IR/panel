package com.parazit.panel.application.telegram.command;

import java.time.Duration;
import java.util.Set;

public record GetTelegramUpdatesCommand(
        long offset,
        int limit,
        Duration timeout,
        Set<String> allowedUpdates
) {

    public GetTelegramUpdatesCommand {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        if (timeout == null || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        allowedUpdates = allowedUpdates == null ? Set.of("message", "callback_query") : Set.copyOf(allowedUpdates);
    }
}
