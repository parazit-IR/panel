package com.parazit.panel.domain.telegram;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "telegram_polling_state")
@EntityListeners(AuditingEntityListener.class)
public class TelegramPollingState {

    public static final int BOT_IDENTITY_MAX_LENGTH = 100;

    @Id
    @Column(name = "bot_identity", nullable = false, length = BOT_IDENTITY_MAX_LENGTH)
    private String botIdentity;

    @Column(name = "next_offset", nullable = false)
    private long nextOffset;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TelegramPollingState() {
    }

    private TelegramPollingState(String botIdentity, long nextOffset) {
        this.botIdentity = normalizeIdentity(botIdentity);
        if (nextOffset < 0) {
            throw new IllegalArgumentException("nextOffset must be non-negative");
        }
        this.nextOffset = nextOffset;
    }

    public static TelegramPollingState create(String botIdentity) {
        return new TelegramPollingState(botIdentity, 0);
    }

    public void advanceAfter(long updateId) {
        if (updateId < 0) {
            throw new IllegalArgumentException("updateId must be non-negative");
        }
        long candidate = Math.addExact(updateId, 1);
        if (candidate > nextOffset) {
            nextOffset = candidate;
        }
    }

    public String getBotIdentity() {
        return botIdentity;
    }

    public long getNextOffset() {
        return nextOffset;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeIdentity(String botIdentity) {
        Objects.requireNonNull(botIdentity, "botIdentity must not be null");
        String normalized = botIdentity.trim();
        if (normalized.isBlank() || normalized.length() > BOT_IDENTITY_MAX_LENGTH) {
            throw new IllegalArgumentException("botIdentity must be nonblank and at most " + BOT_IDENTITY_MAX_LENGTH);
        }
        return normalized;
    }
}
