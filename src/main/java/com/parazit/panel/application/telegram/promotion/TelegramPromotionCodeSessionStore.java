package com.parazit.panel.application.telegram.promotion;

import com.parazit.panel.config.properties.PromotionProperties;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TelegramPromotionCodeSessionStore {

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    private final PromotionProperties properties;

    public TelegramPromotionCodeSessionStore(PromotionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public void startDiscount(long telegramUserId, UUID orderId) {
        sessions.put(telegramUserId, new Session(SessionType.DISCOUNT, Objects.requireNonNull(orderId), Instant.now().plus(properties.reservationTtl())));
    }

    public void startGift(long telegramUserId) {
        sessions.put(telegramUserId, new Session(SessionType.GIFT, null, Instant.now().plus(properties.reservationTtl())));
    }

    public Optional<Session> active(long telegramUserId) {
        Session session = sessions.get(telegramUserId);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(telegramUserId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void clear(long telegramUserId) {
        sessions.remove(telegramUserId);
    }

    public enum SessionType {
        DISCOUNT,
        GIFT
    }

    public record Session(SessionType type, UUID orderId, Instant expiresAt) {
    }
}
