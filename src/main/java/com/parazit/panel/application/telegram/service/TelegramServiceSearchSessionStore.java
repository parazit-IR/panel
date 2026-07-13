package com.parazit.panel.application.telegram.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TelegramServiceSearchSessionStore {

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public void start(long telegramUserId, int returnPage, Instant expiresAt) {
        sessions.put(telegramUserId, new Session(returnPage, expiresAt));
    }

    public Optional<Session> active(long telegramUserId, Instant now) {
        Session session = sessions.get(telegramUserId);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(now)) {
            sessions.remove(telegramUserId, session);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public boolean expired(long telegramUserId, Instant now) {
        Session session = sessions.get(telegramUserId);
        if (session == null || session.expiresAt().isAfter(now)) {
            return false;
        }
        sessions.remove(telegramUserId, session);
        return true;
    }

    public void clear(long telegramUserId) {
        sessions.remove(telegramUserId);
    }

    public record Session(int returnPage, Instant expiresAt) {
    }
}
