package com.parazit.panel.application.telegram.wallet;

import com.parazit.panel.application.port.out.SystemClockPort;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TelegramWalletTopUpAmountSessionStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final Map<Long, Instant> sessions = new ConcurrentHashMap<>();
    private final SystemClockPort clock;

    public TelegramWalletTopUpAmountSessionStore(SystemClockPort clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void start(long telegramUserId) {
        sessions.put(telegramUserId, clock.now().plus(DEFAULT_TTL));
    }

    public Optional<Instant> active(long telegramUserId) {
        Instant expiresAt = sessions.get(telegramUserId);
        if (expiresAt == null) {
            return Optional.empty();
        }
        if (!expiresAt.isAfter(clock.now())) {
            sessions.remove(telegramUserId, expiresAt);
            return Optional.empty();
        }
        return Optional.of(expiresAt);
    }

    public void clear(long telegramUserId) {
        sessions.remove(telegramUserId);
    }
}
